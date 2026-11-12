package com.transitandtile.repository;

import com.transitandtile.entity.Booking;
import com.transitandtile.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find booking by unique reference code.
     */
    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Find booking by reference and case-insensitive email for secure verification.
     */
    Optional<Booking> findByBookingReferenceAndEmailIgnoreCase(String bookingReference, String email);

    /**
     * List all bookings for a room category.
     */
    List<Booking> findByRoomTypeId(Long roomTypeId);

    /**
     * Count overlapping confirmed bookings for a room category during the requested date window.
     * Stays overlap when: existing.checkIn < requestedCheckOut AND existing.checkOut > requestedCheckIn.
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.roomType.id = :roomTypeId " +
           "AND b.status = :status " +
           "AND b.checkIn < :requestedCheckOut " +
           "AND b.checkOut > :requestedCheckIn")
    long countOverlappingBookings(
        @Param("roomTypeId") Long roomTypeId,
        @Param("status") BookingStatus status,
        @Param("requestedCheckIn") LocalDate requestedCheckIn,
        @Param("requestedCheckOut") LocalDate requestedCheckOut
    );
}
