package com.transitandtile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitandtile.repository.BookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-slice booking integration test running against H2 in-memory database.
 *
 * NOTE: H2 integration tests cover the logic and HTTP layer end-to-end
 * but do NOT prove MySQL InnoDB pessimistic locking behavior.
 * Concurrency is validated separately in BookingConcurrencyTest against real MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:booking_integration_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
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
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @AfterEach
    void cleanup() {
        // Clean up test bookings after each test to avoid cross-test interference.
        // The seeded data (hotels, room types) is left intact.
        bookingRepository.deleteAll();
    }

    private static final LocalDate CHECK_IN = LocalDate.now().plusDays(30);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    private String validBookingJson(long roomTypeId, String checkIn, String checkOut) {
        return objectMapper.valueToTree(Map.of(
                "roomTypeId", roomTypeId,
                "customerName", "Integration Test Guest",
                "email", "integtest@example.com",
                "phone", "+919876543210",
                "checkIn", checkIn,
                "checkOut", checkOut
        )).toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 201 Created - valid booking (H2)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: valid request persists booking and returns 201 with correct response")
    void shouldCreateBookingAndReturn201() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson(1L, CHECK_IN.toString(), CHECK_OUT.toString())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingReference", notNullValue()))
                .andExpect(jsonPath("$.bookingReference", matchesPattern("TT-[A-Z]+-\\d{4}")))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.hotelName", is("The Grand Heritage Haveli")))
                .andExpect(jsonPath("$.roomName", is("Royal Deluxe Haveli Room")))
                .andExpect(jsonPath("$.customerName", is("Integration Test Guest")))
                .andExpect(jsonPath("$.email", is("integtest@example.com")))
                .andExpect(jsonPath("$.nights", is(3)))
                .andExpect(jsonPath("$.pricePerNight", is(6500.00)))
                .andExpect(jsonPath("$.totalAmount", is(19500.00)))
                .andExpect(jsonPath("$.checkIn", is(CHECK_IN.toString())))
                .andExpect(jsonPath("$.checkOut", is(CHECK_OUT.toString())))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/bookings: email is normalized (lowercased) in response")
    void shouldNormalizeEmailInResponse() throws Exception {
        String json = objectMapper.valueToTree(Map.of(
                "roomTypeId", 1,
                "customerName", "Test Guest",
                "email", "TEST.GUEST@EXAMPLE.COM",
                "phone", "+919876543210",
                "checkIn", CHECK_IN.toString(),
                "checkOut", CHECK_OUT.toString()
        )).toString();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("test.guest@example.com")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 400 Bad Request - invalid fields
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: blank name and invalid email returns 400 VALIDATION_ERROR")
    void shouldReturn400OnInvalidRequest() throws Exception {
        String json = objectMapper.valueToTree(Map.of(
                "roomTypeId", 1,
                "customerName", "",
                "email", "not-valid",
                "phone", "+919876543210",
                "checkIn", CHECK_IN.toString(),
                "checkOut", CHECK_OUT.toString()
        )).toString();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("POST /api/bookings: check-out before check-in returns 400 INVALID_DATE_RANGE")
    void shouldReturn400WhenCheckOutBeforeCheckIn() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson(1L,
                                CHECK_IN.plusDays(5).toString(),
                                CHECK_IN.toString()))) // checkOut < checkIn
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATE_RANGE")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 Not Found - unknown room type
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: unknown roomTypeId returns 404")
    void shouldReturn404ForUnknownRoomType() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson(99999L, CHECK_IN.toString(), CHECK_OUT.toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("HOTEL_NOT_FOUND")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 Conflict - sold out
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings: sold-out room type (all rooms pre-booked) returns 409 NO_ROOMS_AVAILABLE")
    void shouldReturn409WhenRoomTypeIsFullyBooked() throws Exception {
        // Room type 5 (hotel 5: Lakeview Serenade Palace) has 7 rooms.
        // Book all 7 rooms for the same date range, then attempt an 8th.
        LocalDate soldOutIn = LocalDate.now().plusDays(60);
        LocalDate soldOutOut = soldOutIn.plusDays(2);

        // Book all 7 rooms
        for (int i = 0; i < 7; i++) {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.valueToTree(Map.of(
                                    "roomTypeId", 5,
                                    "customerName", "Guest " + i,
                                    "email", "guest" + i + "@example.com",
                                    "phone", "+919876543210",
                                    "checkIn", soldOutIn.toString(),
                                    "checkOut", soldOutOut.toString()
                            )).toString()))
                    .andExpect(status().isCreated());
        }

        // 8th request should be 409
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.valueToTree(Map.of(
                                "roomTypeId", 5,
                                "customerName", "Overflow Guest",
                                "email", "overflow@example.com",
                                "phone", "+919876543210",
                                "checkIn", soldOutIn.toString(),
                                "checkOut", soldOutOut.toString()
                        )).toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("NO_ROOMS_AVAILABLE")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lookup and Cancel Integration Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings/lookup: retrieves created booking with matching reference and email")
    void shouldLookupBookingSuccessfully() throws Exception {
        // Create a booking first
        String createJson = validBookingJson(1L, CHECK_IN.toString(), CHECK_OUT.toString());
        String responseStr = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref = objectMapper.readTree(responseStr).get("bookingReference").asText();

        // Lookup with matching email
        String lookupJson = objectMapper.valueToTree(Map.of(
                "bookingReference", ref,
                "email", "integtest@example.com"
        )).toString();

        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference", is(ref)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Lookup with mismatching email should return 404 with code BOOKING_NOT_FOUND
        String mismatchJson = objectMapper.valueToTree(Map.of(
                "bookingReference", ref,
                "email", "wrong@example.com"
        )).toString();

        mockMvc.perform(post("/api/bookings/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("BOOKING_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Booking not found.")));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{reference}/cancel: cancels booking, returns BOOKING_NOT_FOUND on mismatch, and rejects repeated cancel")
    void shouldCancelBookingSuccessfully() throws Exception {
        // Create a booking first
        String createJson = validBookingJson(1L, CHECK_IN.toString(), CHECK_OUT.toString());
        String responseStr = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref = objectMapper.readTree(responseStr).get("bookingReference").asText();

        // Mismatched email cancel should return 404 with code BOOKING_NOT_FOUND
        String mismatchCancelJson = objectMapper.valueToTree(Map.of(
                "email", "wrong@example.com"
        )).toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/bookings/" + ref + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchCancelJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("BOOKING_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Booking not found.")));

        // Cancel booking
        String cancelJson = objectMapper.valueToTree(Map.of(
                "email", "integtest@example.com"
        )).toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/bookings/" + ref + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference", is(ref)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Repeated cancel should return 409
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/bookings/" + ref + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BOOKING_STATE_CONFLICT")));
    }
}
