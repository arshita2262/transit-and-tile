package com.transitandtile.service;

import com.transitandtile.dto.request.BookingRequest;
import com.transitandtile.dto.response.BookingResponse;
import com.transitandtile.entity.*;
import com.transitandtile.exception.InvalidDateRangeException;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.exception.RoomNotAvailableException;
import com.transitandtile.repository.BookingRepository;
import com.transitandtile.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 * Uses mocked repositories — does NOT prove MySQL InnoDB locking semantics.
 * Concurrency is validated separately in BookingConcurrencyTest against real MySQL.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingReferenceGenerator referenceGenerator;

    private BookingService bookingService;

    private Hotel hotel;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(roomTypeRepository, bookingRepository, referenceGenerator);

        hotel = new Hotel("Test Hotel", "Candolim, Goa", "A beautiful hotel",
                java.util.List.of("Pool", "Wi-Fi"), new BigDecimal("4.5"),
                java.util.List.of("/img/test.jpg"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                "Free cancellation within 24 hours");
        // Simulate JPA-generated ID
        hotel.setId(1L);

        roomType = new RoomType(hotel, "Deluxe Room", "Ocean view room",
                new BigDecimal("5000.00"), 2, 5);
        roomType.setId(1L);
        hotel.setRoomType(roomType);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Successful booking scenarios
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: 3-night booking calculates correct total and returns CONFIRMED status")
    void shouldCreateBookingWithCorrectTotalAndStatus() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(3); // 3 nights
        BigDecimal expected = new BigDecimal("15000.00"); // 5000 * 3

        BookingRequest request = new BookingRequest(1L, "Priya Sharma",
                "priya@example.com", "+919876543210", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(eq(1L), eq(BookingStatus.CONFIRMED),
                eq(checkIn), eq(checkOut))).thenReturn(0L);
        when(referenceGenerator.generate("Candolim, Goa")).thenReturn("TT-GOA-4891");
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response.getBookingReference()).isEqualTo("TT-GOA-4891");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getNights()).isEqualTo(3L);
        assertThat(response.getPricePerNight()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expected);
        assertThat(response.getHotelName()).isEqualTo("Test Hotel");
        assertThat(response.getRoomName()).isEqualTo("Deluxe Room");
        assertThat(response.getCustomerName()).isEqualTo("Priya Sharma");
    }

    @Test
    @DisplayName("createBooking: 1-night booking calculates correct single-night total")
    void shouldCalculateOneNightTotal() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(1);

        BookingRequest request = new BookingRequest(1L, "Rahul Kumar",
                "rahul@example.com", "+919876543211", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(0L);
        when(referenceGenerator.generate(any())).thenReturn("TT-GOA-1234");
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(101L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response.getNights()).isEqualTo(1L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("createBooking: email is normalized (trimmed and lowercased) before persisting")
    void shouldNormalizeEmailBeforePersisting() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "  Test.Email@EXAMPLE.COM  ", "+919876543210", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(0L);
        when(referenceGenerator.generate(any())).thenReturn("TT-GOA-5678");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.saveAndFlush(captor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(102L);
            return b;
        });

        bookingService.createBooking(request);

        assertThat(captor.getValue().getEmail()).isEqualTo("test.email@example.com");
    }

    @Test
    @DisplayName("createBooking: price is loaded from DB, never from client")
    void shouldUseDatabasePriceNotClientPrice() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(0L);
        when(referenceGenerator.generate(any())).thenReturn("TT-GOA-9999");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.saveAndFlush(captor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(103L);
            return b;
        });

        bookingService.createBooking(request);

        // DB price is 5000.00; the entity should store this, not any client value
        assertThat(captor.getValue().getPricePerNightAtBooking())
                .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date validation failures
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: check-in in the past throws InvalidDateRangeException")
    void shouldRejectPastCheckIn() {
        LocalDate pastCheckIn = LocalDate.now().minusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", pastCheckIn, checkOut);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("cannot be in the past");
    }

    @Test
    @DisplayName("createBooking: check-out on same day as check-in throws InvalidDateRangeException")
    void shouldRejectCheckOutSameDayAsCheckIn() {
        LocalDate date = LocalDate.now().plusDays(5);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", date, date);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("strictly after check-in");
    }

    @Test
    @DisplayName("createBooking: check-out before check-in throws InvalidDateRangeException")
    void shouldRejectCheckOutBeforeCheckIn() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.minusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("strictly after check-in");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resource not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: nonexistent room type throws ResourceNotFoundException")
    void shouldThrowNotFoundForUnknownRoomType() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(9999L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inventory exhausted
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: all rooms booked throws RoomNotAvailableException (409)")
    void shouldRejectWhenInventoryExhausted() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        // All 5 rooms are booked
        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(5L);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessageContaining("No rooms are available");
    }

    @Test
    @DisplayName("createBooking: exactly totalRooms overlapping rejects (boundary: >= not >)")
    void shouldRejectWhenOverlappingEqualsTotal() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        // Exactly 5 out of 5 rooms occupied
        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any()))
                .thenReturn((long) roomType.getTotalRooms());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    @DisplayName("createBooking: CANCELLED bookings are not counted (count returns 0 for CONFIRMED)")
    void cancelledBookingsShouldNotConsumeInventory() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Test Guest",
                "test@example.com", "+919876543210", checkIn, checkOut);

        // 0 CONFIRMED bookings (cancelled ones are not passed to this query)
        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(eq(1L), eq(BookingStatus.CONFIRMED),
                any(), any())).thenReturn(0L);
        when(referenceGenerator.generate(any())).thenReturn("TT-GOA-7777");
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(104L);
            return b;
        });

        // Should succeed because only CONFIRMED bookings are counted
        assertThatCode(() -> bookingService.createBooking(request))
                .doesNotThrowAnyException();

        // Verify query was called with CONFIRMED status only
        verify(bookingRepository).countOverlappingBookings(1L, BookingStatus.CONFIRMED,
                checkIn, checkOut);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reference collision retry
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: retries on reference collision and succeeds on second attempt")
    void shouldRetryOnReferenceCollisionAndSucceed() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingRequest request = new BookingRequest(1L, "Retry Guest",
                "retry@example.com", "+919876543210", checkIn, checkOut);

        when(roomTypeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(roomType));
        when(bookingRepository.countOverlappingBookings(any(), any(), any(), any())).thenReturn(0L);
        when(referenceGenerator.generate(any()))
                .thenReturn("TT-GOA-1111")  // attempt 1 - collides
                .thenReturn("TT-GOA-2222"); // attempt 2 - succeeds

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate booking_reference"))
                .thenAnswer(inv -> {
                    Booking b = inv.getArgument(0);
                    b.setId(105L);
                    return b;
                });

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response.getBookingReference()).isEqualTo("TT-GOA-2222");
        verify(bookingRepository, times(2)).saveAndFlush(any(Booking.class));
        verify(referenceGenerator, times(2)).generate(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lookup and Cancellation tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lookupBooking: returns booking when reference and email match")
    void shouldLookupBookingWhenReferenceAndEmailMatch() {
        Booking booking = new Booking(roomType, "TT-GOA-1234", "Priya Sharma",
                "priya@example.com", "+919876543210",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8),
                new BigDecimal("5000.00"), new BigDecimal("15000.00"),
                BookingStatus.CONFIRMED, null);
        booking.setId(10L);

        when(bookingRepository.findByBookingReferenceAndEmailIgnoreCase("TT-GOA-1234", "priya@example.com"))
                .thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.lookupBooking(
                new com.transitandtile.dto.request.BookingLookupRequest("TT-GOA-1234", "  PRIYA@EXAMPLE.COM "));

        assertThat(response.getBookingReference()).isEqualTo("TT-GOA-1234");
        assertThat(response.getCustomerName()).isEqualTo("Priya Sharma");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("lookupBooking: throws BookingNotFoundException when reference or email does not match")
    void shouldThrowBookingNotFoundExceptionWhenLookupFails() {
        when(bookingRepository.findByBookingReferenceAndEmailIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.lookupBooking(
                new com.transitandtile.dto.request.BookingLookupRequest("TT-WRONG-0000", "wrong@example.com")))
                .isInstanceOf(com.transitandtile.exception.BookingNotFoundException.class)
                .hasMessage("Booking not found.");
    }

    @Test
    @DisplayName("cancelBooking: updates status to CANCELLED and returns updated response")
    void shouldCancelConfirmedBooking() {
        Booking booking = new Booking(roomType, "TT-GOA-1234", "Priya Sharma",
                "priya@example.com", "+919876543210",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8),
                new BigDecimal("5000.00"), new BigDecimal("15000.00"),
                BookingStatus.CONFIRMED, null);
        booking.setId(10L);

        when(bookingRepository.findByBookingReferenceAndEmailIgnoreCase("TT-GOA-1234", "priya@example.com"))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.cancelBooking("TT-GOA-1234",
                new com.transitandtile.dto.request.BookingCancelRequest("priya@example.com"));

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException when reference or email does not match")
    void shouldThrowBookingNotFoundExceptionWhenCancelFails() {
        when(bookingRepository.findByBookingReferenceAndEmailIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking("TT-WRONG-0000",
                new com.transitandtile.dto.request.BookingCancelRequest("wrong@example.com")))
                .isInstanceOf(com.transitandtile.exception.BookingNotFoundException.class)
                .hasMessage("Booking not found.");
    }

    @Test
    @DisplayName("cancelBooking: throws BookingConflictException when booking is already CANCELLED")
    void shouldRejectCancellingAlreadyCancelledBooking() {
        Booking booking = new Booking(roomType, "TT-GOA-1234", "Priya Sharma",
                "priya@example.com", "+919876543210",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8),
                new BigDecimal("5000.00"), new BigDecimal("15000.00"),
                BookingStatus.CANCELLED, null);
        booking.setId(10L);

        when(bookingRepository.findByBookingReferenceAndEmailIgnoreCase("TT-GOA-1234", "priya@example.com"))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("TT-GOA-1234",
                new com.transitandtile.dto.request.BookingCancelRequest("priya@example.com")))
                .isInstanceOf(com.transitandtile.exception.BookingConflictException.class)
                .hasMessageContaining("already cancelled");
    }
}
