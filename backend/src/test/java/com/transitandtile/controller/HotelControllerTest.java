package com.transitandtile.controller;

import com.transitandtile.dto.response.AvailabilityResponse;
import com.transitandtile.dto.response.HotelCardResponse;
import com.transitandtile.dto.response.HotelDetailResponse;
import com.transitandtile.dto.response.RoomTypeResponse;
import com.transitandtile.exception.GlobalExceptionHandler;
import com.transitandtile.exception.InvalidDateRangeException;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.service.AvailabilityService;
import com.transitandtile.service.HotelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
@Import(GlobalExceptionHandler.class)
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HotelService hotelService;

    @MockBean
    private AvailabilityService availabilityService;

    @Test
    @DisplayName("GET /api/hotels?location=Goa should return matching hotels")
    void shouldReturnHotelsByLocation() throws Exception {
        HotelCardResponse card = new HotelCardResponse(
                2L,
                "Azure Sands Beach Resort",
                "Candolim, Goa",
                "Beachfront luxury",
                List.of("Beachfront Access", "Pool"),
                new BigDecimal("4.7"),
                List.of("https://example.com/goa.jpg"),
                new BigDecimal("5200.00"),
                "Deluxe Ocean Vista Suite",
                2
        );

        when(hotelService.searchHotels("Goa")).thenReturn(List.of(card));

        mockMvc.perform(get("/api/hotels")
                        .param("location", "Goa")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)))
                .andExpect(jsonPath("$[0].name", is("Azure Sands Beach Resort")))
                .andExpect(jsonPath("$[0].location", is("Candolim, Goa")))
                .andExpect(jsonPath("$[0].pricePerNight", is(5200.00)))
                .andExpect(jsonPath("$[0].roomName", is("Deluxe Ocean Vista Suite")));
    }

    @Test
    @DisplayName("GET /api/hotels without location param should return all hotels")
    void shouldReturnAllHotels() throws Exception {
        HotelCardResponse h1 = new HotelCardResponse(1L, "Hotel One", "Jaipur", "Desc",
                List.of(), new BigDecimal("4.8"), List.of(), new BigDecimal("6500.00"), "Suite", 2);
        HotelCardResponse h2 = new HotelCardResponse(2L, "Hotel Two", "Goa", "Desc",
                List.of(), new BigDecimal("4.7"), List.of(), new BigDecimal("5200.00"), "Deluxe", 2);

        when(hotelService.searchHotels(null)).thenReturn(List.of(h1, h2));

        mockMvc.perform(get("/api/hotels")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    @DisplayName("GET /api/hotels/{id} for existing hotel should return hotel details")
    void shouldReturnHotelDetails() throws Exception {
        RoomTypeResponse room = new RoomTypeResponse(
                1L, "Royal Deluxe Haveli Room", "Spacious room",
                new BigDecimal("6500.00"), 2, 8
        );
        HotelDetailResponse detail = new HotelDetailResponse(
                1L,
                "The Grand Heritage Haveli",
                "Jaipur, Rajasthan",
                "Authentic 18th-century Rajput haveli",
                List.of("Free Wi-Fi", "Swimming Pool"),
                new BigDecimal("4.8"),
                List.of("https://example.com/img1.jpg"),
                LocalTime.of(14, 0),
                LocalTime.of(11, 0),
                "Free cancellation up to 48 hours",
                room
        );

        when(hotelService.getHotelDetails(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/hotels/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("The Grand Heritage Haveli")))
                .andExpect(jsonPath("$.location", is("Jaipur, Rajasthan")))
                .andExpect(jsonPath("$.rating", is(4.8)))
                .andExpect(jsonPath("$.roomType.roomName", is("Royal Deluxe Haveli Room")))
                .andExpect(jsonPath("$.roomType.pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$.roomType.totalRooms", is(8)));
    }

    @Test
    @DisplayName("GET /api/hotels/{id} for unknown ID should return 404 with structured JSON")
    void shouldReturn404ForUnknownHotel() throws Exception {
        when(hotelService.getHotelDetails(999L))
                .thenThrow(new ResourceNotFoundException("Hotel", 999L));

        mockMvc.perform(get("/api/hotels/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("HOTEL_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Hotel not found with id: 999")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/hotels/{id}/availability with valid dates returns 200 and AvailabilityResponse")
    void shouldReturnAvailabilityWithValidDates() throws Exception {
        LocalDate in = LocalDate.of(2026, 12, 1);
        LocalDate out = LocalDate.of(2026, 12, 4);

        AvailabilityResponse resp = new AvailabilityResponse(
                1L, "The Grand Heritage Haveli", 10L, "Royal Deluxe Haveli Room",
                2, in, out, 3, 8, 2, 6, true, "Available",
                new BigDecimal("6500.00"), new BigDecimal("19500.00")
        );

        when(availabilityService.checkAvailability(eq(1L), eq(in), eq(out))).thenReturn(resp);

        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-04")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotelId", is(1)))
                .andExpect(jsonPath("$.roomTypeId", is(10)))
                .andExpect(jsonPath("$.roomName", is("Royal Deluxe Haveli Room")))
                .andExpect(jsonPath("$.nights", is(3)))
                .andExpect(jsonPath("$.totalRooms", is(8)))
                .andExpect(jsonPath("$.bookedRooms", is(2)))
                .andExpect(jsonPath("$.availableRooms", is(6)))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.status", is("Available")))
                .andExpect(jsonPath("$.pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$.estimatedTotalPrice", is(19500.00)));
    }

    @Test
    @DisplayName("7. Invalid date range returns 400 Bad Request with INVALID_DATE_RANGE")
    void shouldReturn400ForInvalidDateRange() throws Exception {
        LocalDate in = LocalDate.of(2026, 12, 5);
        LocalDate out = LocalDate.of(2026, 12, 1);

        when(availabilityService.checkAvailability(eq(1L), eq(in), eq(out)))
                .thenThrow(new InvalidDateRangeException("Check-out date must be strictly after check-in date."));

        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", "2026-12-05")
                        .param("checkOut", "2026-12-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_DATE_RANGE")))
                .andExpect(jsonPath("$.message", containsString("Check-out date must be strictly after check-in date.")));
    }

    @Test
    @DisplayName("8. Invalid date format returns 400 Bad Request with INVALID_REQUEST_PARAMETER")
    void shouldReturn400ForInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", "invalid-date-format")
                        .param("checkOut", "2026-12-04")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST_PARAMETER")))
                .andExpect(jsonPath("$.message", containsString("Expected format: YYYY-MM-DD")));
    }

    @Test
    @DisplayName("9. Non-existent hotel ID returns 404 Not Found with HOTEL_NOT_FOUND")
    void shouldReturn404ForNonexistentHotelAvailability() throws Exception {
        LocalDate in = LocalDate.of(2026, 12, 1);
        LocalDate out = LocalDate.of(2026, 12, 4);

        when(availabilityService.checkAvailability(eq(999L), eq(in), eq(out)))
                .thenThrow(new ResourceNotFoundException("Hotel", 999L));

        mockMvc.perform(get("/api/hotels/999/availability")
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-04")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("HOTEL_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Hotel not found with id: 999")));
    }
}
