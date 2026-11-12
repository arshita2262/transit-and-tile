package com.transitandtile.controller;

import com.transitandtile.dto.request.BookingCancelRequest;
import com.transitandtile.dto.request.BookingLookupRequest;
import com.transitandtile.dto.request.BookingRequest;
import com.transitandtile.dto.response.BookingResponse;
import com.transitandtile.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for booking operations:
 * - POST  /api/bookings                  (creation)
 * - POST  /api/bookings/lookup           (retrieval with reference + email)
 * - PATCH /api/bookings/{reference}/cancel (cancellation with email)
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     * Creates a confirmed one-room booking.
     *
     * @param request validated booking request (roomTypeId, customerName, email, phone, checkIn, checkOut)
     * @return 201 Created with BookingResponse containing backend-authoritative confirmation details
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/bookings/lookup
     * Retrieves a booking using booking reference and customer email.
     *
     * @param request validated lookup request (bookingReference, email)
     * @return 200 OK with BookingResponse
     */
    @PostMapping("/lookup")
    public ResponseEntity<BookingResponse> lookupBooking(@Valid @RequestBody BookingLookupRequest request) {
        BookingResponse response = bookingService.lookupBooking(request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/bookings/{reference}/cancel
     * Cancels a confirmed booking after verifying customer email.
     *
     * @param reference booking reference path variable
     * @param request validated cancel request (email)
     * @return 200 OK with updated BookingResponse
     */
    @PatchMapping("/{reference}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("reference") String reference,
                                                         @Valid @RequestBody BookingCancelRequest request) {
        BookingResponse response = bookingService.cancelBooking(reference, request);
        return ResponseEntity.ok(response);
    }
}
