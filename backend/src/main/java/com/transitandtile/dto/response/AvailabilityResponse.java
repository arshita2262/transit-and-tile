package com.transitandtile.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AvailabilityResponse {

    private Long hotelId;
    private String hotelName;
    private Long roomTypeId;
    private String roomName;
    private Integer capacity;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long nights;
    private int totalRooms;
    private int bookedRooms;
    private int availableRooms;
    private boolean available;
    private String status;
    private BigDecimal pricePerNight;
    private BigDecimal estimatedTotalPrice;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(Long hotelId, String hotelName, Long roomTypeId, String roomName,
                                Integer capacity, LocalDate checkIn, LocalDate checkOut,
                                long nights, int totalRooms, int bookedRooms, int availableRooms,
                                boolean available, String status, BigDecimal pricePerNight,
                                BigDecimal estimatedTotalPrice) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.roomTypeId = roomTypeId;
        this.roomName = roomName;
        this.capacity = capacity;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.totalRooms = totalRooms;
        this.bookedRooms = bookedRooms;
        this.availableRooms = availableRooms;
        this.available = available;
        this.status = status;
        this.pricePerNight = pricePerNight;
        this.estimatedTotalPrice = estimatedTotalPrice;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public long getNights() {
        return nights;
    }

    public void setNights(long nights) {
        this.nights = nights;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getBookedRooms() {
        return bookedRooms;
    }

    public void setBookedRooms(int bookedRooms) {
        this.bookedRooms = bookedRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public BigDecimal getEstimatedTotalPrice() {
        return estimatedTotalPrice;
    }

    public void setEstimatedTotalPrice(BigDecimal estimatedTotalPrice) {
        this.estimatedTotalPrice = estimatedTotalPrice;
    }
}
