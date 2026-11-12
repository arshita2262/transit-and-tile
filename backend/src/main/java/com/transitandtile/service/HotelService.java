package com.transitandtile.service;

import com.transitandtile.dto.response.HotelCardResponse;
import com.transitandtile.dto.response.HotelDetailResponse;
import com.transitandtile.dto.response.RoomTypeResponse;
import com.transitandtile.entity.Hotel;
import com.transitandtile.entity.RoomType;
import com.transitandtile.exception.ResourceNotFoundException;
import com.transitandtile.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /**
     * Search hotels by destination location. If location is blank or null,
     * returns all available hotels.
     */
    public List<HotelCardResponse> searchHotels(String location) {
        List<Hotel> hotels;
        if (location != null && !location.trim().isEmpty()) {
            hotels = hotelRepository.findByLocationContainingIgnoreCase(location.trim());
        } else {
            hotels = hotelRepository.findAll();
        }

        return hotels.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complete details for a specific hotel by ID.
     * Throws ResourceNotFoundException if no matching hotel exists.
     */
    public HotelDetailResponse getHotelDetails(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));

        return toDetailResponse(hotel);
    }

    private HotelCardResponse toCardResponse(Hotel hotel) {
        RoomType room = hotel.getRoomType();
        BigDecimal price = room != null ? room.getPricePerNight() : null;
        String roomName = room != null ? room.getRoomName() : null;
        Integer capacity = room != null ? room.getCapacity() : null;

        return new HotelCardResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                hotel.getAmenities(),
                hotel.getRating(),
                hotel.getImageUrls(),
                price,
                roomName,
                capacity
        );
    }

    private HotelDetailResponse toDetailResponse(Hotel hotel) {
        RoomTypeResponse roomResponse = null;
        if (hotel.getRoomType() != null) {
            RoomType r = hotel.getRoomType();
            roomResponse = new RoomTypeResponse(
                    r.getId(),
                    r.getRoomName(),
                    r.getDescription(),
                    r.getPricePerNight(),
                    r.getCapacity(),
                    r.getTotalRooms()
            );
        }

        return new HotelDetailResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                hotel.getAmenities(),
                hotel.getRating(),
                hotel.getImageUrls(),
                hotel.getCheckInTime(),
                hotel.getCheckOutTime(),
                hotel.getCancellationPolicy(),
                roomResponse
        );
    }
}
