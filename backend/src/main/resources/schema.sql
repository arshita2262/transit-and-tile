-- =============================================================================
-- Transit & Tile — Schema DDL
-- Tables: HOTEL, ROOM_TYPE, BOOKING
-- Engine: InnoDB
-- =============================================================================

CREATE TABLE IF NOT EXISTS hotel (
    hotel_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    amenities TEXT,
    rating DECIMAL(2,1) NOT NULL,
    image_urls TEXT,
    check_in_time TIME NOT NULL,
    check_out_time TIME NOT NULL,
    cancellation_policy TEXT NOT NULL,
    INDEX idx_hotel_location (location)
);

CREATE TABLE IF NOT EXISTS room_type (
    room_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id BIGINT NOT NULL UNIQUE,
    room_name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    capacity INT NOT NULL,
    total_rooms INT NOT NULL,
    CONSTRAINT fk_room_type_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id),
    CONSTRAINT chk_room_price CHECK (price_per_night > 0),
    CONSTRAINT chk_room_capacity CHECK (capacity > 0),
    CONSTRAINT chk_room_total CHECK (total_rooms > 0)
);

CREATE TABLE IF NOT EXISTS booking (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    booking_reference VARCHAR(30) NOT NULL UNIQUE,
    customer_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(25) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    price_per_night_at_booking DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (room_type_id),
    CONSTRAINT chk_booking_dates CHECK (check_out > check_in),
    CONSTRAINT chk_booking_total CHECK (total_amount >= 0),
    INDEX idx_booking_reference (booking_reference),
    INDEX idx_booking_availability (room_type_id, status, check_in, check_out),
    INDEX idx_booking_email (email)
);
