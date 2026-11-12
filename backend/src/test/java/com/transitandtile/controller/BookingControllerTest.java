package com.transitandtile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitandtile.dto.request.BookingRequest;
import com.transitandtile.dto.response.BookingResponse;
import com.transitandtile.exception.InvalidDateRangeException;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.exception.RoomNotAvailableException;
import com.transitandtile.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice test for BookingController.
 * Uses @WebMvcTest to test the HTTP layer with BookingService mocked.
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private static final LocalDate FUTURE_CHECK_IN = LocalDate.now().plusDays(10);
    private static final LocalDate FUTURE_CHECK_OUT = LocalDate.now().plusDays(13);

    private BookingRequest validRequest() {
        return new BookingRequest(1L, "Priya Sharma", "priya@example.com",
                "+919876543210", FUTURE_CHECK_IN, FUTURE_CHECK_OUT);
    }

    private BookingResponse sampleResponse() {
        return new BookingResponse(
                "TT-GOA-4891", "CONFIRMED",
                1L, "Test Hotel", "Candolim, Goa",
                1L, "Deluxe Room",
                "Priya Sharma", "priya@example.com", "+919876543210",
                FUTURE_CHECK_IN, FUTURE_CHECK_OUT,
                3L, new BigDecimal("5000.00"), new BigDecimal("15000.00"),
                LocalDateTime.now()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 201 Created - success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: valid request returns 201 with booking reference and correct fields")
    void shouldReturn201OnSuccessfulBooking() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingReference", is("TT-GOA-4891")))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.hotelName", is("Test Hotel")))
                .andExpect(jsonPath("$.roomName", is("Deluxe Room")))
                .andExpect(jsonPath("$.customerName", is("Priya Sharma")))
                .andExpect(jsonPath("$.nights", is(3)))
                .andExpect(jsonPath("$.pricePerNight", is(5000.00)))
                .andExpect(jsonPath("$.totalAmount", is(15000.00)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 400 Bad Request - validation failures
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: blank customer name returns 400 VALIDATION_ERROR")
    void shouldReturn400WhenCustomerNameBlank() throws Exception {
        BookingRequest req = new BookingRequest(1L, "",
                "priya@example.com", "+919876543210", FUTURE_CHECK_IN, FUTURE_CHECK_OUT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors", hasItem(containsString("customerName"))));
    }

    @Test
    @DisplayName("POST /api/bookings: invalid email format returns 400 VALIDATION_ERROR")
    void shouldReturn400WhenEmailInvalid() throws Exception {
        BookingRequest req = new BookingRequest(1L, "Priya Sharma",
                "not-an-email", "+919876543210", FUTURE_CHECK_IN, FUTURE_CHECK_OUT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors", hasItem(containsString("email"))));
    }

    @Test
    @DisplayName("POST /api/bookings: missing roomTypeId returns 400 VALIDATION_ERROR")
    void shouldReturn400WhenRoomTypeIdMissing() throws Exception {
        BookingRequest req = new BookingRequest(null, "Priya Sharma",
                "priya@example.com", "+919876543210", FUTURE_CHECK_IN, FUTURE_CHECK_OUT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors", hasItem(containsString("roomTypeId"))));
    }

    @Test
    @DisplayName("POST /api/bookings: missing checkIn returns 400 VALIDATION_ERROR")
    void shouldReturn400WhenCheckInMissing() throws Exception {
        BookingRequest req = new BookingRequest(1L, "Priya Sharma",
                "priya@example.com", "+919876543210", null, FUTURE_CHECK_OUT);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("POST /api/bookings: past check-in date (service-layer) returns 400 INVALID_DATE_RANGE")
    void shouldReturn400WhenCheckInInPast() throws Exception {
        BookingRequest req = new BookingRequest(1L, "Priya Sharma",
                "priya@example.com", "+919876543210",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2));

        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new InvalidDateRangeException("Check-in date cannot be in the past."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATE_RANGE")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 Not Found - unknown room type
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: unknown roomTypeId returns 404 HOTEL_NOT_FOUND")
    void shouldReturn404WhenRoomTypeNotFound() throws Exception {
        BookingRequest req = new BookingRequest(9999L, "Priya Sharma",
                "priya@example.com", "+919876543210", FUTURE_CHECK_IN, FUTURE_CHECK_OUT);

        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new ResourceNotFoundException("Room type", 9999L));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("HOTEL_NOT_FOUND")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 Conflict - no inventory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: sold-out room type returns 409 NO_ROOMS_AVAILABLE")
    void shouldReturn409WhenRoomSoldOut() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new RoomNotAvailableException("No rooms are available for the selected dates."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("NO_ROOMS_AVAILABLE")))
                .andExpect(jsonPath("$.message", containsString("No rooms are available")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 Not Found - lookup & cancel mismatch (BOOKING_NOT_FOUND)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings/lookup: mismatch returns 404 BOOKING_NOT_FOUND")
    void shouldReturn404BookingNotFoundOnLookupMismatch() throws Exception {
        when(bookingService.lookupBooking(any()))
                .thenThrow(new com.transitandtile.exception.BookingNotFoundException());

        String json = objectMapper.valueToTree(java.util.Map.of(
                "bookingReference", "TT-NOTFOUND-0000",
                "email", "wrong@example.com"
        )).toString();

        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("BOOKING_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Booking not found.")));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{reference}/cancel: mismatch returns 404 BOOKING_NOT_FOUND")
    void shouldReturn404BookingNotFoundOnCancelMismatch() throws Exception {
        when(bookingService.cancelBooking(any(), any()))
                .thenThrow(new com.transitandtile.exception.BookingNotFoundException());

        String json = objectMapper.valueToTree(java.util.Map.of(
                "email", "wrong@example.com"
        )).toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/bookings/TT-NOTFOUND-0000/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("BOOKING_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Booking not found.")));
    }
}
