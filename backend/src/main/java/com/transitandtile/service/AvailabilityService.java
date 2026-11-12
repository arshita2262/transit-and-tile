package com.transitandtile.service;

import com.transitandtile.dto.response.AvailabilityResponse;
import com.transitandtile.entity.BookingStatus;
import com.transitandtile.entity.Hotel;
import com.transitandtile.entity.RoomType;
import com.transitandtile.exception.InvalidDateRangeException;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.repository.BookingRepository;
import com.transitandtile.repository.HotelRepository;
import com.transitandtile.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    public static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Kolkata");

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityService(HotelRepository hotelRepository,
                               RoomTypeRepository roomTypeRepository,
                               BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Checks availability for a specific hotel and date window.
     *
     * Overlap rule:
     * existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn
     *
     * Check-out day is available for another guest's check-in (half-open interval).
     * Only CONFIRMED bookings count; CANCELLED bookings are excluded.
     *
     * @param hotelId  ID of the hotel to check
     * @param checkIn  requested check-in date
     * @param checkOut requested check-out date
     * @return AvailabilityResponse with accurate counts and calculated pricing
     */
    public AvailabilityResponse checkAvailability(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        validateStayDates(checkIn, checkOut);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));

        RoomType roomType = hotel.getRoomType();
        if (roomType == null) {
            roomType = roomTypeRepository.findByHotelId(hotelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room type for hotel", hotelId));
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        int totalRooms = roomType.getTotalRooms();

        long overlappingBookings = bookingRepository.countOverlappingBookings(
                roomType.getId(),
                BookingStatus.CONFIRMED,
                checkIn,
                checkOut
        );

        int bookedRooms = (int) overlappingBookings;
        int availableRooms = Math.max(0, totalRooms - bookedRooms);
        boolean isAvailable = availableRooms >= 1;
        String status = isAvailable ? "Available" : "Sold Out";

        BigDecimal pricePerNight = roomType.getPricePerNight();
        BigDecimal estimatedTotalPrice = pricePerNight
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);

        return new AvailabilityResponse(
                hotel.getId(),
                hotel.getName(),
                roomType.getId(),
                roomType.getRoomName(),
                roomType.getCapacity(),
                checkIn,
                checkOut,
                nights,
                totalRooms,
                bookedRooms,
                availableRooms,
                isAvailable,
                status,
                pricePerNight,
                estimatedTotalPrice
        );
    }

    /**
     * Validates stay dates according to the authoritative business rules:
     * - Dates must be non-null.
     * - Check-in cannot be before current date in local date context.
     * - Check-out must be strictly after check-in.
     */
    public void validateStayDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidDateRangeException("Both check-in and check-out dates are required.");
        }

        LocalDate today = LocalDate.now(LOCAL_ZONE);
        if (checkIn.isBefore(today)) {
            throw new InvalidDateRangeException("Check-in date cannot be in the past. Current date is: " + today);
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidDateRangeException("Check-out date must be strictly after check-in date.");
        }
    }
}
