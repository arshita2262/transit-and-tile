package com.transitandtile.repository;

import com.transitandtile.entity.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    /**
     * Retrieve the unique room type assigned to a specific hotel.
     */
    Optional<RoomType> findByHotelId(Long hotelId);

    /**
     * Retrieve a room type by ID with a pessimistic write lock.
     * Used in booking creation to serialize concurrent requests for the same room type.
     * This lock is only meaningful against MySQL/InnoDB in a real transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomType r WHERE r.id = :id")
    Optional<RoomType> findByIdWithLock(@Param("id") Long id);
}
