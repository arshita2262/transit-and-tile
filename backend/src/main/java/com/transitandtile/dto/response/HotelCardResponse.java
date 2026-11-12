package com.transitandtile.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class HotelCardResponse {

    private Long id;
    private String name;
    private String location;
    private String description;
    private List<String> amenities;
    private BigDecimal rating;
    private List<String> imageUrls;
    private BigDecimal pricePerNight;
    private String roomName;
    private Integer capacity;

    public HotelCardResponse() {
    }

    public HotelCardResponse(Long id, String name, String location, String description,
                             List<String> amenities, BigDecimal rating, List<String> imageUrls,
                             BigDecimal pricePerNight, String roomName, Integer capacity) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.rating = rating;
        this.imageUrls = imageUrls;
        this.pricePerNight = pricePerNight;
        this.roomName = roomName;
        this.capacity = capacity;
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

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
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
}
