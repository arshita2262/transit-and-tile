package com.transitandtile.concurrency;

import com.transitandtile.dto.request.BookingRequest;
import com.transitandtile.entity.Booking;
import com.transitandtile.entity.BookingStatus;
import com.transitandtile.entity.Hotel;
import com.transitandtile.entity.RoomType;
import com.transitandtile.exception.RoomNotAvailableException;
import com.transitandtile.repository.BookingRepository;
import com.transitandtile.repository.HotelRepository;
import com.transitandtile.repository.RoomTypeRepository;
import com.transitandtile.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL/InnoDB concurrency test for booking creation.
 *
 * IMPORTANT: This test runs against the REAL MySQL database (transit_and_tile_db).
 * It is tagged as "concurrency" and uses a dedicated test hotel/room type
 * that is fully cleaned up in @AfterEach — it does NOT touch the seeded hotels.
 *
 * Purpose: validate that pessimistic write lock on ROOM_TYPE prevents overbooking
 * when multiple threads attempt to book the same room simultaneously.
 *
 * H2 integration tests (BookingIntegrationTest) cover logic and HTTP layer
 * but cannot prove InnoDB locking semantics. This test does.
 *
 * Run conditions: DB_USERNAME=root, DB_PASSWORD=password must be set in environment.
 */
@SpringBootTest
@Tag("concurrency")
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Hotel testHotel;
    private RoomType testRoomType;

    private static final int TOTAL_TEST_ROOMS = 1; // 1 room = at most 1 successful booking
    private static final int CONCURRENT_THREADS = 10;
    private static final LocalDate TEST_CHECK_IN = LocalDate.now().plusDays(90);
    private static final LocalDate TEST_CHECK_OUT = TEST_CHECK_IN.plusDays(2);

    @BeforeEach
    void setUpTestData() {
        // Create an isolated hotel not part of the seeded data
        testHotel = hotelRepository.save(new Hotel(
                "CONCURRENCY_TEST_HOTEL_DO_NOT_USE",
                "Test Location, TestCity",
                "Temporary hotel for concurrency test - safe to delete",
                List.of("Test"),
                new BigDecimal("4.0"),
                List.of("/img/test.jpg"),
                LocalTime.of(14, 0),
                LocalTime.of(11, 0),
                "Test cancellation policy"
        ));

        // Create a room type with exactly 1 room
        testRoomType = roomTypeRepository.save(new RoomType(
                testHotel,
                "Concurrency Test Room",
                "Single room for concurrency test",
                new BigDecimal("3000.00"),
                2,
                TOTAL_TEST_ROOMS // only 1 room
        ));
    }

    @AfterEach
    void cleanUpTestData() {
        // Delete all bookings for the test room type first (FK constraint)
        List<Booking> testBookings = bookingRepository.findByRoomTypeId(testRoomType.getId());
        bookingRepository.deleteAll(testBookings);

        // Delete test room type and hotel
        roomTypeRepository.delete(testRoomType);
        hotelRepository.delete(testHotel);
    }

    @Test
    @DisplayName("MySQL InnoDB: 10 concurrent threads booking 1-room hotel → exactly 1 succeeds, 9 get 409")
    void shouldAllowOnlyOneBookingWhenOnlyOneRoomAvailable() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);  // all threads start simultaneously
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrors = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait until all threads are ready
                    BookingRequest request = new BookingRequest(
                            testRoomType.getId(),
                            "ConcurrentGuest" + threadIndex,
                            "concurrent" + threadIndex + "@test.com",
                            "+919876543210",
                            TEST_CHECK_IN,
                            TEST_CHECK_OUT
                    );
                    bookingService.createBooking(request);
                    successCount.incrementAndGet();
                } catch (RoomNotAvailableException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrors.incrementAndGet();
                    errors.add(threadIndex + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads simultaneously
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all threads completed within timeout
        assertThat(completed).as("All concurrent threads must complete within 30 seconds").isTrue();

        // Verify exactly 1 booking succeeded
        assertThat(successCount.get())
                .as("Exactly 1 booking should succeed when only 1 room is available")
                .isEqualTo(1);

        // Verify 9 were rejected with RoomNotAvailableException
        assertThat(conflictCount.get())
                .as("Exactly 9 bookings should be rejected with RoomNotAvailableException")
                .isEqualTo(CONCURRENT_THREADS - 1);

        // No unexpected errors
        assertThat(unexpectedErrors.get())
                .as("No unexpected errors: " + errors)
                .isEqualTo(0);

        // Verify the actual DB has exactly 1 CONFIRMED booking
        long confirmedCount = bookingRepository.countOverlappingBookings(
                testRoomType.getId(), BookingStatus.CONFIRMED, TEST_CHECK_IN, TEST_CHECK_OUT);
        assertThat(confirmedCount)
                .as("Database must contain exactly 1 CONFIRMED booking")
                .isEqualTo(1L);
    }
}
