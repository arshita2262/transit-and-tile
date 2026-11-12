package com.transitandtile.repository;

import com.transitandtile.entity.Booking;
import com.transitandtile.entity.BookingStatus;
import com.transitandtile.entity.Hotel;
import com.transitandtile.entity.RoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.sql.init.mode=never",
    "DB_USERNAME=sa",
    "DB_PASSWORD="
})
class RepositoryIntegrationTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Should persist and retrieve hotel with JSON amenities and image URLs")
    void shouldPersistAndRetrieveHotel() {
        Hotel hotel = new Hotel(
            "Test Boutique Stay",
            "Udaipur, Rajasthan",
            "A serene boutique hotel overlooking the lake.",
            List.of("Free Wi-Fi", "Swimming Pool", "Rooftop Restaurant"),
            new BigDecimal("4.8"),
            List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg"),
            LocalTime.of(14, 0),
            LocalTime.of(11, 0),
            "Free cancellation up to 48 hours."
        );

        Hotel saved = hotelRepository.save(hotel);
        assertNotNull(saved.getId());

        Optional<Hotel> found = hotelRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Boutique Stay", found.get().getName());
        assertEquals(3, found.get().getAmenities().size());
        assertEquals(2, found.get().getImageUrls().size());
    }

    @Test
    @DisplayName("Should query hotels by destination case-insensitively")
    void shouldQueryHotelsByLocation() {
        Hotel h1 = new Hotel("Goa Beach Villa", "Candolim, Goa", "Beachside stay",
            List.of("Wi-Fi"), new BigDecimal("4.5"), List.of(), LocalTime.of(14, 0), LocalTime.of(11, 0), "Flexible");
        Hotel h2 = new Hotel("Jaipur Haveli", "Jaipur, Rajasthan", "Heritage stay",
            List.of("Wi-Fi"), new BigDecimal("4.7"), List.of(), LocalTime.of(14, 0), LocalTime.of(11, 0), "Flexible");

        hotelRepository.saveAll(List.of(h1, h2));

        List<Hotel> goaHotels = hotelRepository.findByLocationContainingIgnoreCase("goa");
        assertEquals(1, goaHotels.size());
        assertEquals("Goa Beach Villa", goaHotels.get(0).getName());

        List<Hotel> rajHotels = hotelRepository.findByLocationContainingIgnoreCase("RAJASTHAN");
        assertEquals(1, rajHotels.size());
        assertEquals("Jaipur Haveli", rajHotels.get(0).getName());
    }

    @Test
    @DisplayName("Should enforce 1-to-1 relationship between Hotel and RoomType")
    void shouldLinkHotelAndRoomType() {
        Hotel hotel = hotelRepository.save(new Hotel(
            "Manali Retreat", "Manali, HP", "Mountain lodge",
            List.of("Heating"), new BigDecimal("4.6"), List.of(),
            LocalTime.of(13, 0), LocalTime.of(10, 30), "Policy"
        ));

        RoomType roomType = new RoomType(
            hotel, "Deluxe Mountain View", "Warm cedar wood room",
            new BigDecimal("3500.00"), 2, 5
        );
        RoomType savedRoom = roomTypeRepository.save(roomType);

        Optional<RoomType> found = roomTypeRepository.findByHotelId(hotel.getId());
        assertTrue(found.isPresent());
        assertEquals("Deluxe Mountain View", found.get().getRoomName());
        assertEquals(new BigDecimal("3500.00"), found.get().getPricePerNight());
    }

    @Test
    @DisplayName("Should persist Booking with historical price snapshot and count overlaps")
    void shouldPersistBookingWithPriceSnapshot() {
        Hotel hotel = hotelRepository.save(new Hotel(
            "Kerala Palms", "Munnar, Kerala", "Tea plantation resort",
            List.of("Tea Tour"), new BigDecimal("4.9"), List.of(),
            LocalTime.of(14, 0), LocalTime.of(11, 0), "Policy"
        ));

        RoomType room = roomTypeRepository.save(new RoomType(
            hotel, "Estate Chalet", "Plush room",
            new BigDecimal("4000.00"), 2, 4
        ));

        Booking booking = new Booking(
            room,
            "TT-MUN-1001",
            "Ananya Sharma",
            "ananya.sharma@example.com",
            "+919876543210",
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 4),
            new BigDecimal("4000.00"),
            new BigDecimal("12000.00"),
            BookingStatus.CONFIRMED,
            LocalDateTime.now()
        );

        Booking savedBooking = bookingRepository.save(booking);
        assertNotNull(savedBooking.getId());
        assertEquals(new BigDecimal("4000.00"), savedBooking.getPricePerNightAtBooking());
        assertEquals(new BigDecimal("12000.00"), savedBooking.getTotalAmount());

        // Overlap verification: stay [2026-12-02 to 2026-12-05] overlaps [2026-12-01 to 2026-12-04]
        long countOverlap = bookingRepository.countOverlappingBookings(
            room.getId(),
            BookingStatus.CONFIRMED,
            LocalDate.of(2026, 12, 2),
            LocalDate.of(2026, 12, 5)
        );
        assertEquals(1, countOverlap);

        // Boundary test: check-out equals check-in [2026-12-04 to 2026-12-07] does NOT overlap
        long countAdjacent = bookingRepository.countOverlappingBookings(
            room.getId(),
            BookingStatus.CONFIRMED,
            LocalDate.of(2026, 12, 4),
            LocalDate.of(2026, 12, 7)
        );
        assertEquals(0, countAdjacent);

        // Cancelled bookings do not count
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        long countAfterCancel = bookingRepository.countOverlappingBookings(
            room.getId(),
            BookingStatus.CONFIRMED,
            LocalDate.of(2026, 12, 2),
            LocalDate.of(2026, 12, 5)
        );
        assertEquals(0, countAfterCancel);
    }
}
