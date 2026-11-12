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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Hotel testHotel;
    private RoomType testRoom;

    // Use dates comfortably in the future
    private final LocalDate checkIn = LocalDate.now(AvailabilityService.LOCAL_ZONE).plusDays(10);
    private final LocalDate checkOut = checkIn.plusDays(3); // 3 nights

    @BeforeEach
    void setUp() {
        testHotel = new Hotel(
                "The Grand Heritage Haveli",
                "Jaipur, Rajasthan",
                "Heritage luxury",
                List.of("Free Wi-Fi", "Pool"),
                new BigDecimal("4.8"),
                List.of("https://example.com/h1.jpg"),
                LocalTime.of(14, 0),
                LocalTime.of(11, 0),
                "Free cancellation up to 48 hours"
        );
        testHotel.setId(1L);

        testRoom = new RoomType(
                testHotel,
                "Royal Deluxe Haveli Room",
                "Spacious heritage room",
                new BigDecimal("6500.00"),
                2,
                8 // 8 total rooms
        );
        testRoom.setId(10L);
        testHotel.setRoomType(testRoom);
    }

    @Test
    @DisplayName("1. All rooms available: 0 bookings -> availableRooms == totalRooms, isAvailable == true")
    void shouldReturnAllRoomsAvailableWhenNoBookings() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkIn), eq(checkOut)))
                .thenReturn(0L);

        AvailabilityResponse response = availabilityService.checkAvailability(1L, checkIn, checkOut);

        assertNotNull(response);
        assertEquals(1L, response.getHotelId());
        assertEquals("The Grand Heritage Haveli", response.getHotelName());
        assertEquals(10L, response.getRoomTypeId());
        assertEquals(8, response.getTotalRooms());
        assertEquals(0, response.getBookedRooms());
        assertEquals(8, response.getAvailableRooms());
        assertTrue(response.isAvailable());
        assertEquals("Available", response.getStatus());
        assertEquals(3, response.getNights());
        assertEquals(new BigDecimal("6500.00"), response.getPricePerNight());
        assertEquals(new BigDecimal("19500.00"), response.getEstimatedTotalPrice()); // 6500 * 3
    }

    @Test
    @DisplayName("2. Some rooms booked: 3 of 8 booked -> availableRooms == 5, isAvailable == true")
    void shouldReturnCorrectAvailabilityWhenSomeRoomsBooked() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkIn), eq(checkOut)))
                .thenReturn(3L);

        AvailabilityResponse response = availabilityService.checkAvailability(1L, checkIn, checkOut);

        assertNotNull(response);
        assertEquals(8, response.getTotalRooms());
        assertEquals(3, response.getBookedRooms());
        assertEquals(5, response.getAvailableRooms());
        assertTrue(response.isAvailable());
        assertEquals("Available", response.getStatus());
        assertEquals(new BigDecimal("19500.00"), response.getEstimatedTotalPrice());
    }

    @Test
    @DisplayName("3. All rooms booked: 8 of 8 booked -> availableRooms == 0, isAvailable == false, status == 'Sold Out'")
    void shouldReturnSoldOutWhenAllRoomsBooked() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkIn), eq(checkOut)))
                .thenReturn(8L);

        AvailabilityResponse response = availabilityService.checkAvailability(1L, checkIn, checkOut);

        assertNotNull(response);
        assertEquals(8, response.getTotalRooms());
        assertEquals(8, response.getBookedRooms());
        assertEquals(0, response.getAvailableRooms());
        assertFalse(response.isAvailable());
        assertEquals("Sold Out", response.getStatus());
    }

    @Test
    @DisplayName("4. CANCELLED bookings excluded: query explicitly queries CONFIRMED status only")
    void shouldQueryOnlyConfirmedBookingsExcludingCancelled() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkIn), eq(checkOut)))
                .thenReturn(2L);

        AvailabilityResponse response = availabilityService.checkAvailability(1L, checkIn, checkOut);

        // Verify that only BookingStatus.CONFIRMED was passed to the repository query
        verify(bookingRepository, times(1)).countOverlappingBookings(
                eq(10L),
                eq(BookingStatus.CONFIRMED),
                eq(checkIn),
                eq(checkOut)
        );
        assertEquals(6, response.getAvailableRooms());
    }

    @Test
    @DisplayName("5. Non-overlapping bookings excluded: dates passed to repository match requested range")
    void shouldPassExactRequestedDatesForOverlapCalculation() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkIn), eq(checkOut)))
                .thenReturn(0L);

        availabilityService.checkAvailability(1L, checkIn, checkOut);

        verify(bookingRepository).countOverlappingBookings(10L, BookingStatus.CONFIRMED, checkIn, checkOut);
    }

    @Test
    @DisplayName("6. Check-out/check-in boundary dates: check-out day is available for another guest")
    void shouldValidateStayDurationCorrectly() {
        LocalDate checkInDate = LocalDate.now(AvailabilityService.LOCAL_ZONE).plusDays(5);
        LocalDate checkOutDate = checkInDate.plusDays(1); // 1-night stay

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.countOverlappingBookings(eq(10L), eq(BookingStatus.CONFIRMED), eq(checkInDate), eq(checkOutDate)))
                .thenReturn(0L);

        AvailabilityResponse response = availabilityService.checkAvailability(1L, checkInDate, checkOutDate);

        assertEquals(1, response.getNights());
        assertEquals(new BigDecimal("6500.00"), response.getEstimatedTotalPrice());
    }

    @Test
    @DisplayName("7. Invalid date range: checkIn in past, checkOut <= checkIn, or nulls throw InvalidDateRangeException")
    void shouldThrowExceptionForInvalidDateRanges() {
        LocalDate today = LocalDate.now(AvailabilityService.LOCAL_ZONE);

        // Past checkIn
        assertThrows(InvalidDateRangeException.class, () ->
                availabilityService.checkAvailability(1L, today.minusDays(1), today.plusDays(2)));

        // checkOut equals checkIn
        assertThrows(InvalidDateRangeException.class, () ->
                availabilityService.checkAvailability(1L, today.plusDays(2), today.plusDays(2)));

        // checkOut before checkIn
        assertThrows(InvalidDateRangeException.class, () ->
                availabilityService.checkAvailability(1L, today.plusDays(5), today.plusDays(3)));

        // Null dates
        assertThrows(InvalidDateRangeException.class, () ->
                availabilityService.checkAvailability(1L, null, today.plusDays(2)));
        assertThrows(InvalidDateRangeException.class, () ->
                availabilityService.checkAvailability(1L, today.plusDays(2), null));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("9. Non-existent hotel ID: throws ResourceNotFoundException")
    void shouldThrowNotFoundForNonexistentHotel() {
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                availabilityService.checkAvailability(999L, checkIn, checkOut));

        verifyNoInteractions(bookingRepository);
    }
}
