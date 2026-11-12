package com.transitandtile.service;

import com.transitandtile.dto.request.BookingCancelRequest;
import com.transitandtile.dto.request.BookingLookupRequest;
import com.transitandtile.dto.request.BookingRequest;
import com.transitandtile.dto.response.BookingResponse;
import com.transitandtile.entity.Booking;
import com.transitandtile.entity.BookingStatus;
import com.transitandtile.entity.Hotel;
import com.transitandtile.entity.RoomType;
import com.transitandtile.exception.BookingConflictException;
import com.transitandtile.exception.BookingNotFoundException;
import com.transitandtile.exception.InvalidDateRangeException;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.exception.RoomNotAvailableException;
import com.transitandtile.repository.BookingRepository;
import com.transitandtile.repository.RoomTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Service for creating bookings.
 *
 * Uses a pessimistic write lock on the ROOM_TYPE row to prevent overbooking.
 * All availability checks, price calculations, and inserts happen inside a single transaction.
 *
 * IMPORTANT: Pessimistic locking is only effective against MySQL/InnoDB in production.
 * H2 integration tests cover the logic path but do NOT prove InnoDB locking semantics.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MAX_REFERENCE_RETRIES = 5;

    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;
    private final BookingReferenceGenerator referenceGenerator;

    public BookingService(RoomTypeRepository roomTypeRepository,
                          BookingRepository bookingRepository,
                          BookingReferenceGenerator referenceGenerator) {
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
        this.referenceGenerator = referenceGenerator;
    }

    /**
     * Creates a confirmed booking for one room.
     *
     * Transaction steps (all within a single @Transactional boundary):
     * 1. Validate dates.
     * 2. Acquire pessimistic write lock on ROOM_TYPE row.
     * 3. Recheck availability after acquiring the lock.
     * 4. Calculate nights and total amount using the database price (never client price).
     * 5. Generate a unique booking reference with bounded collision retry.
     * 6. Persist and return the booking DTO.
     *
     * @param request validated booking request DTO
     * @return BookingResponse with backend-authoritative confirmation details
     * @throws InvalidDateRangeException  if dates are invalid
     * @throws ResourceNotFoundException  if the room type does not exist
     * @throws RoomNotAvailableException  if no rooms are available for the requested dates
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        validateDates(request.getCheckIn(), request.getCheckOut());

        // Acquire pessimistic write lock on the room type row.
        // This serializes competing booking requests for the same room type under InnoDB.
        RoomType roomType = roomTypeRepository.findByIdWithLock(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type", request.getRoomTypeId()));

        Hotel hotel = roomType.getHotel();

        // Recheck availability while holding the lock.
        long overlapping = bookingRepository.countOverlappingBookings(
                roomType.getId(),
                BookingStatus.CONFIRMED,
                request.getCheckIn(),
                request.getCheckOut()
        );

        if (overlapping >= roomType.getTotalRooms()) {
            throw new RoomNotAvailableException(
                    "No rooms are available for the selected dates. All " +
                    roomType.getTotalRooms() + " room(s) are booked."
            );
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal pricePerNight = roomType.getPricePerNight().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Booking booking = persistBookingWithRetry(
                roomType, request, normalizedEmail, pricePerNight, totalAmount, hotel.getLocation()
        );

        log.info("Booking created: reference={}, hotel={}, roomType={}, nights={}, total={}",
                booking.getBookingReference(), hotel.getName(), roomType.getRoomName(), nights, totalAmount);

        return toResponse(booking, hotel, roomType, nights);
    }

    /**
     * Retrieves a booking using booking reference and customer email.
     * Both must match for retrieval (security verification).
     *
     * @param request lookup request containing reference and email
     * @return BookingResponse
     * @throws ResourceNotFoundException if reference or email do not match
     */
    @Transactional(readOnly = true)
    public BookingResponse lookupBooking(BookingLookupRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Booking booking = bookingRepository.findByBookingReferenceAndEmailIgnoreCase(
                request.getBookingReference().trim(), normalizedEmail
        ).orElseThrow(BookingNotFoundException::new);

        RoomType roomType = booking.getRoomType();
        Hotel hotel = roomType.getHotel();
        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());

        return toResponse(booking, hotel, roomType, nights);
    }

    /**
     * Cancels a confirmed booking after verifying customer email.
     * Updates status to CANCELLED (does not delete row).
     * Releases room inventory for availability calculation.
     *
     * @param reference booking reference code
     * @param request cancel request containing email
     * @return updated BookingResponse
     * @throws BookingNotFoundException if reference/email not found
     * @throws BookingConflictException if booking is already cancelled
     */
    @Transactional
    public BookingResponse cancelBooking(String reference, BookingCancelRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Booking booking = bookingRepository.findByBookingReferenceAndEmailIgnoreCase(
                reference.trim(), normalizedEmail
        ).orElseThrow(BookingNotFoundException::new);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException("Booking is already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);

        RoomType roomType = updated.getRoomType();
        Hotel hotel = roomType.getHotel();
        long nights = ChronoUnit.DAYS.between(updated.getCheckIn(), updated.getCheckOut());

        log.info("Booking cancelled: reference={}", reference);
        return toResponse(updated, hotel, roomType, nights);
    }

    /**
     * Persists the booking, retrying on booking-reference collision (DataIntegrityViolationException).
     * Retries are bounded to MAX_REFERENCE_RETRIES to prevent infinite loops.
     */
    private Booking persistBookingWithRetry(RoomType roomType, BookingRequest request,
                                            String normalizedEmail, BigDecimal pricePerNight,
                                            BigDecimal totalAmount, String hotelLocation) {
        for (int attempt = 1; attempt <= MAX_REFERENCE_RETRIES; attempt++) {
            String reference = referenceGenerator.generate(hotelLocation);
            Booking booking = new Booking(
                    roomType,
                    reference,
                    request.getCustomerName().trim(),
                    normalizedEmail,
                    request.getPhone().trim(),
                    request.getCheckIn(),
                    request.getCheckOut(),
                    pricePerNight,
                    totalAmount,
                    BookingStatus.CONFIRMED,
                    LocalDateTime.now(LOCAL_ZONE)
            );
            try {
                return bookingRepository.saveAndFlush(booking);
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_REFERENCE_RETRIES) {
                    log.error("Booking reference collision exhausted {} retries for location '{}'",
                            MAX_REFERENCE_RETRIES, hotelLocation);
                    throw new RoomNotAvailableException(
                            "Could not generate a unique booking reference. Please try again.");
                }
                log.warn("Booking reference collision on attempt {} (reference={}), retrying...",
                        attempt, reference);
            }
        }
        // Unreachable, but satisfies compiler
        throw new RoomNotAvailableException("Could not generate a unique booking reference. Please try again.");
    }

    /**
     * Maps a Booking entity plus its associated hotel and room to a BookingResponse DTO.
     * Never returns JPA entities to the controller.
     */
    private BookingResponse toResponse(Booking booking, Hotel hotel, RoomType roomType, long nights) {
        return new BookingResponse(
                booking.getBookingReference(),
                booking.getStatus().name(),
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                roomType.getId(),
                roomType.getRoomName(),
                booking.getCustomerName(),
                booking.getEmail(),
                booking.getPhone(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                nights,
                booking.getPricePerNightAtBooking(),
                booking.getTotalAmount(),
                booking.getCreatedAt()
        );
    }

    /**
     * Validates stay dates using the same business rules as AvailabilityService.
     * - Both dates are required.
     * - Check-in cannot be in the past (India/Kolkata date context).
     * - Check-out must be strictly after check-in.
     */
    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidDateRangeException("Both check-in and check-out dates are required.");
        }
        LocalDate today = LocalDate.now(LOCAL_ZONE);
        if (checkIn.isBefore(today)) {
            throw new InvalidDateRangeException(
                    "Check-in date cannot be in the past. Current date is: " + today);
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidDateRangeException(
                    "Check-out date must be strictly after check-in date.");
        }
    }
}
