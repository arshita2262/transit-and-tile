package com.transitandtile.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Inbound DTO for POST /api/bookings.
 * The client must supply room type, dates, and customer contact details.
 * Price, booking reference, and status are NEVER accepted from the client.
 */
public class BookingRequest {

    @NotNull(message = "Room type ID is required")
    @Positive(message = "Room type ID must be a positive number")
    private Long roomTypeId;

    @NotBlank(message = "Customer name is required")
    @Size(max = 120, message = "Customer name must not exceed 120 characters")
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[+]?[0-9\\-\\s()]{7,25}$",
        message = "Phone must be 7-25 characters and contain only digits, spaces, +, -, or parentheses"
    )
    @Size(max = 25, message = "Phone must not exceed 25 characters")
    private String phone;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOut;

    public BookingRequest() {
    }

    public BookingRequest(Long roomTypeId, String customerName, String email,
                          String phone, LocalDate checkIn, LocalDate checkOut) {
        this.roomTypeId = roomTypeId;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }
}
