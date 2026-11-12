package com.transitandtile.exception;

/**
 * Thrown when an invalid booking state transition is attempted (e.g., cancelling an already cancelled booking).
 * Maps to HTTP 409 Conflict.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
