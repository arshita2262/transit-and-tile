package com.transitandtile.controller;

import com.transitandtile.dto.response.AvailabilityResponse;
import com.transitandtile.dto.response.HotelCardResponse;
import com.transitandtile.dto.response.HotelDetailResponse;
import com.transitandtile.service.AvailabilityService;
import com.transitandtile.service.HotelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final AvailabilityService availabilityService;

    public HotelController(HotelService hotelService, AvailabilityService availabilityService) {
        this.hotelService = hotelService;
        this.availabilityService = availabilityService;
    }

    /**
     * GET /api/hotels?location=...
     * Searches hotels by destination location. If location is omitted, returns all hotels.
     */
    @GetMapping
    public ResponseEntity<List<HotelCardResponse>> searchHotels(
            @RequestParam(name = "location", required = false) String location) {
        List<HotelCardResponse> results = hotelService.searchHotels(location);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/hotels/{hotelId}
     * Returns full details for the specified hotel ID.
     */
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDetailResponse> getHotelDetails(@PathVariable("hotelId") Long hotelId) {
        HotelDetailResponse response = hotelService.getHotelDetails(hotelId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/hotels/{hotelId}/availability?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD
     * Recalculates real room availability and estimated pricing for the specified date window.
     */
    @GetMapping("/{hotelId}/availability")
    public ResponseEntity<AvailabilityResponse> getHotelAvailability(
            @PathVariable("hotelId") Long hotelId,
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        AvailabilityResponse response = availabilityService.checkAvailability(hotelId, checkIn, checkOut);
        return ResponseEntity.ok(response);
    }
}
