package com.transitandtile.dto.response;

import java.math.BigDecimal;

public class RoomTypeResponse {

    private Long id;
    private String roomName;
    private String description;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private Integer totalRooms;

    public RoomTypeResponse() {
    }

    public RoomTypeResponse(Long id, String roomName, String description,
                            BigDecimal pricePerNight, Integer capacity, Integer totalRooms) {
        this.id = id;
        this.roomName = roomName;
        this.description = description;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.totalRooms = totalRooms;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }
}
