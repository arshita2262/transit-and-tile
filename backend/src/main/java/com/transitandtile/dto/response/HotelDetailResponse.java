package com.transitandtile.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class HotelDetailResponse {

    private Long id;
    private String name;
    private String location;
    private String description;
    private List<String> amenities;
    private BigDecimal rating;
    private List<String> imageUrls;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String cancellationPolicy;
    private RoomTypeResponse roomType;

    public HotelDetailResponse() {
    }

    public HotelDetailResponse(Long id, String name, String location, String description,
                               List<String> amenities, BigDecimal rating, List<String> imageUrls,
                               LocalTime checkInTime, LocalTime checkOutTime,
                               String cancellationPolicy, RoomTypeResponse roomType) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.rating = rating;
        this.imageUrls = imageUrls;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.cancellationPolicy = cancellationPolicy;
        this.roomType = roomType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public RoomTypeResponse getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeResponse roomType) {
        this.roomType = roomType;
    }
}
