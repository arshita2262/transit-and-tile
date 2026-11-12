package com.transitandtile.exception;

/**
 * Thrown when a booking cannot be found by booking reference + email during lookup or cancellation.
 * Maps to HTTP 404 with code BOOKING_NOT_FOUND and generic message "Booking not found."
 * to avoid disclosing whether the reference or email was incorrect.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException() {
        super("Booking not found.");
    }

    public BookingNotFoundException(String message) {
        super(message);
    }
}
