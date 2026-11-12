package com.transitandtile.exception;

/**
 * Thrown when no rooms are available for the requested date range.
 * Maps to HTTP 409 Conflict.
 */
public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
