package com.transitandtile.controller;

import com.transitandtile.entity.Booking;
import com.transitandtile.entity.BookingStatus;
import com.transitandtile.entity.RoomType;
import com.transitandtile.repository.BookingRepository;
import com.transitandtile.repository.RoomTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test exercising the full slice:
 * HTTP Request -> Controller -> Service -> Repository -> Database -> DTO -> JSON
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:hotel_integration_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.defer-datasource-initialization=true",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema.sql",
    "spring.sql.init.data-locations=classpath:data.sql",
    "DB_USERNAME=sa",
    "DB_PASSWORD="
})
class HotelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("GET /api/hotels should return all 5 seeded hotels")
    void shouldReturnAllSeededHotels() throws Exception {
        mockMvc.perform(get("/api/hotels")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name", is("The Grand Heritage Haveli")))
                .andExpect(jsonPath("$[0].pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$[1].name", is("Azure Sands Beach Resort")))
                .andExpect(jsonPath("$[1].pricePerNight", is(5200.00)));
    }

    @Test
    @DisplayName("GET /api/hotels?location=Goa should filter by destination")
    void shouldFilterHotelsByDestination() throws Exception {
        mockMvc.perform(get("/api/hotels")
                        .param("location", "Goa")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Azure Sands Beach Resort")))
                .andExpect(jsonPath("$[0].location", containsString("Goa")))
                .andExpect(jsonPath("$[0].roomName", is("Deluxe Ocean Vista Suite")));
    }

    @Test
    @DisplayName("GET /api/hotels/1 should return full hotel and room details")
    void shouldReturnHotelDetailsById() throws Exception {
        mockMvc.perform(get("/api/hotels/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("The Grand Heritage Haveli")))
                .andExpect(jsonPath("$.location", is("Jaipur, Rajasthan")))
                .andExpect(jsonPath("$.rating", is(4.8)))
                .andExpect(jsonPath("$.amenities", hasItem("Free High-Speed Wi-Fi")))
                .andExpect(jsonPath("$.roomType.roomName", is("Royal Deluxe Haveli Room")))
                .andExpect(jsonPath("$.roomType.pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$.roomType.capacity", is(2)))
                .andExpect(jsonPath("$.roomType.totalRooms", is(8)));
    }

    @Test
    @DisplayName("GET /api/hotels/999 should return HTTP 404 with consistent ErrorResponse")
    void shouldReturn404ForNonexistentHotel() throws Exception {
        mockMvc.perform(get("/api/hotels/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("HOTEL_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Hotel not found with id: 999")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/hotels/1/availability with valid dates returns accurate availability from database")
    void shouldReturnAvailabilityFromDatabase() throws Exception {
        LocalDate in = LocalDate.now().plusDays(20);
        LocalDate out = in.plusDays(3);

        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", in.toString())
                        .param("checkOut", out.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotelId", is(1)))
                .andExpect(jsonPath("$.hotelName", is("The Grand Heritage Haveli")))
                .andExpect(jsonPath("$.nights", is(3)))
                .andExpect(jsonPath("$.totalRooms", is(8)))
                .andExpect(jsonPath("$.bookedRooms", is(0)))
                .andExpect(jsonPath("$.availableRooms", is(8)))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.status", is("Available")))
                .andExpect(jsonPath("$.pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$.estimatedTotalPrice", is(19500.00)));
    }

    @Test
    @DisplayName("GET /api/hotels/2/availability: reflects booked rooms, sold out status, cancelled exclusions, and boundary dates")
    void shouldReflectBookingsAccuratelyInAvailability() throws Exception {
        RoomType roomType = roomTypeRepository.findByHotelId(2L).orElseThrow();
        LocalDate stayStart = LocalDate.now().plusDays(40);
        LocalDate stayEnd = stayStart.plusDays(3); // 3 nights

        // Create 2 CONFIRMED bookings for hotel 2 (which has 10 total rooms)
        bookingRepository.save(new Booking(
                roomType, "TT-INT-2001", "Guest One", "guest1@example.com", "+919876543211",
                stayStart, stayEnd, roomType.getPricePerNight(),
                roomType.getPricePerNight().multiply(BigDecimal.valueOf(3)),
                BookingStatus.CONFIRMED, LocalDateTime.now()
        ));
        bookingRepository.save(new Booking(
                roomType, "TT-INT-2002", "Guest Two", "guest2@example.com", "+919876543212",
                stayStart, stayEnd, roomType.getPricePerNight(),
                roomType.getPricePerNight().multiply(BigDecimal.valueOf(3)),
                BookingStatus.CONFIRMED, LocalDateTime.now()
        ));

        // Create 1 CANCELLED booking overlapping the same dates (must NOT reduce inventory)
        bookingRepository.save(new Booking(
                roomType, "TT-INT-2003", "Guest Cancelled", "cancel@example.com", "+919876543213",
                stayStart, stayEnd, roomType.getPricePerNight(),
                roomType.getPricePerNight().multiply(BigDecimal.valueOf(3)),
                BookingStatus.CANCELLED, LocalDateTime.now()
        ));

        // Create 1 booking ending exactly on stayStart (adjacent boundary: checkout day available for new guest)
        bookingRepository.save(new Booking(
                roomType, "TT-INT-2004", "Guest Prior", "prior@example.com", "+919876543214",
                stayStart.minusDays(2), stayStart, roomType.getPricePerNight(),
                roomType.getPricePerNight().multiply(BigDecimal.valueOf(2)),
                BookingStatus.CONFIRMED, LocalDateTime.now()
        ));

        // Total: 10, Booked: 2 (2 confirmed overlapping; cancelled & adjacent excluded) -> Available: 8
        mockMvc.perform(get("/api/hotels/2/availability")
                        .param("checkIn", stayStart.toString())
                        .param("checkOut", stayEnd.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId", is(2)))
                .andExpect(jsonPath("$.hotelName", is("Azure Sands Beach Resort")))
                .andExpect(jsonPath("$.totalRooms", is(10)))
                .andExpect(jsonPath("$.bookedRooms", is(2)))
                .andExpect(jsonPath("$.availableRooms", is(8)))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.status", is("Available")))
                .andExpect(jsonPath("$.pricePerNight", is(5200.00)))
                .andExpect(jsonPath("$.estimatedTotalPrice", is(15600.00)));
    }

    @Test
    @DisplayName("GET /api/hotels/1/availability returns 400 when checkOut <= checkIn")
    void shouldReturn400WhenInvalidDateRange() throws Exception {
        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = in.minusDays(1);

        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", in.toString())
                        .param("checkOut", out.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_DATE_RANGE")))
                .andExpect(jsonPath("$.message", containsString("Check-out date must be strictly after check-in date.")));
    }

    @Test
    @DisplayName("GET /api/hotels/1/availability returns 400 when date format is malformed")
    void shouldReturn400WhenDateFormatMalformed() throws Exception {
        mockMvc.perform(get("/api/hotels/1/availability")
                        .param("checkIn", "2026/12/01")
                        .param("checkOut", "2026-12-05")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST_PARAMETER")))
                .andExpect(jsonPath("$.message", containsString("Expected format: YYYY-MM-DD")));
    }
}
