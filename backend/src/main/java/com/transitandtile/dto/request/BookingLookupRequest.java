package com.transitandtile.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for POST /api/bookings/lookup.
 * Requires both booking reference and email for secure booking retrieval.
 */
public class BookingLookupRequest {

    @NotBlank(message = "Booking reference is required")
    @Size(max = 30, message = "Booking reference must not exceed 30 characters")
    private String bookingReference;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    public BookingLookupRequest() {
    }

    public BookingLookupRequest(String bookingReference, String email) {
        this.bookingReference = bookingReference;
        this.email = email;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
