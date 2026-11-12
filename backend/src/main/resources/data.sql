-- =============================================================================
-- Transit & Tile — Sample Hotel & Room Data (5 Sample Hotels)
-- Uses INSERT IGNORE so re-running does not produce duplicate key errors.
-- =============================================================================

INSERT IGNORE INTO hotel (hotel_id, name, location, description, amenities, rating, image_urls, check_in_time, check_out_time, cancellation_policy)
VALUES
(1,
 'The Grand Heritage Haveli',
 'Jaipur, Rajasthan',
 'An authentic 18th-century Rajput haveli restored into a luxury heritage hotel. Features ornate jharokha balconies, handcrafted marble courtyards, and traditional Rajasthani hospitality in the heart of the Pink City.',
 '["Free High-Speed Wi-Fi", "Swimming Pool", "Rooftop Restaurant", "Spa & Wellness", "Heritage Courtyard", "Airport Shuttle"]',
 4.8,
 '["https://images.unsplash.com/photo-1566073771259-6a8506099945", "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b", "https://images.unsplash.com/photo-1590490360182-c33d57733427"]',
 '14:00:00', '11:00:00',
 'Free cancellation up to 48 hours before check-in. Non-refundable thereafter.');

INSERT IGNORE INTO hotel (hotel_id, name, location, description, amenities, rating, image_urls, check_in_time, check_out_time, cancellation_policy)
VALUES
(2,
 'Azure Sands Beach Resort',
 'Candolim, Goa',
 'Set right on the golden sands of Candolim beach with panoramic Arabian Sea views. Enjoy sunset dining at the beachfront grill, refreshing infinity pool swims, and relaxed coastal vibes.',
 '["Beachfront Access", "Infinity Pool", "Sunset Bar & Grill", "Complimentary Breakfast", "Water Sports Desk", "Free Wi-Fi"]',
 4.7,
 '["https://images.unsplash.com/photo-1520250497591-112f2f40a3f4", "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9", "https://images.unsplash.com/photo-1540555700478-4be289fbecef"]',
 '15:00:00', '11:00:00',
 'Free cancellation up to 24 hours before check-in. One night charged for late cancellation.');

INSERT IGNORE INTO hotel (hotel_id, name, location, description, amenities, rating, image_urls, check_in_time, check_out_time, cancellation_policy)
VALUES
(3,
 'Pinecrest Mountain Lodge',
 'Old Manali, Himachal Pradesh',
 'Nestled among deodar cedar pine groves with breathtaking snowcapped Himalayan views. Features an in-house wood-fired bakery, stone fireplaces, and easy access to trekking trails.',
 '["Mountain View Balcony", "Fireplace Lounge", "In-house Bakery", "Trekking Guide Service", "Heated Rooms", "Free Wi-Fi"]',
 4.6,
 '["https://images.unsplash.com/photo-1542314831-068cd1dbfeeb", "https://images.unsplash.com/photo-1618773928121-c32242e63f39", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461"]',
 '13:00:00', '10:30:00',
 'Free cancellation up to 72 hours before check-in. 50% retention for cancellations within 72 hours.');

INSERT IGNORE INTO hotel (hotel_id, name, location, description, amenities, rating, image_urls, check_in_time, check_out_time, cancellation_policy)
VALUES
(4,
 'Misty Valley Tea Estate Stay',
 'Munnar, Kerala',
 'A tranquil colonial-era bungalow set inside 200 acres of rolling green tea estates. Wake up to misty hills, birdsong, freshly brewed garden tea, and soothing Ayurvedic wellness treatments.',
 '["Tea Plantation Trail", "Ayurvedic Spa", "Organic Garden Dining", "Private Balcony", "Bird Watching Tours", "Free Wi-Fi"]',
 4.9,
 '["https://images.unsplash.com/photo-1584132967334-10e028bd69f7", "https://images.unsplash.com/photo-1591088398332-8a7791972843", "https://images.unsplash.com/photo-1566665797739-1674de7a421a"]',
 '14:00:00', '11:00:00',
 'Free cancellation up to 48 hours before check-in. Full refund if cancelled earlier.');

INSERT IGNORE INTO hotel (hotel_id, name, location, description, amenities, rating, image_urls, check_in_time, check_out_time, cancellation_policy)
VALUES
(5,
 'Lakeview Serenade Palace',
 'Udaipur, Rajasthan',
 'Perched gracefully along Lake Pichola overlooking the City Palace and Jag Mandir. Unwind with regal Mewari architecture, sunset boat cruises, and live evening folk performances.',
 '["Pichola Lake View", "Rooftop Fine Dining", "Cultural Evening Shows", "Swimming Pool", "Heritage Architecture", "Concierge Desk"]',
 4.8,
 '["https://images.unsplash.com/photo-1571896349842-33c89424de2d", "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af", "https://images.unsplash.com/photo-1564501049412-61c2a3083791"]',
 '14:00:00', '12:00:00',
 'Free cancellation up to 48 hours before check-in. Non-refundable thereafter.');

-- -----------------------------------------------------------------------------
-- Room Categories (Exactly one per hotel)
-- -----------------------------------------------------------------------------

INSERT IGNORE INTO room_type (room_type_id, hotel_id, room_name, description, price_per_night, capacity, total_rooms)
VALUES
(1, 1, 'Royal Deluxe Haveli Room',
 'Spacious 420 sq.ft room with authentic jharokha window seat, king-size four-poster bed, heritage brass fittings, and private courtyard view.',
 6500.00, 2, 8);

INSERT IGNORE INTO room_type (room_type_id, hotel_id, room_name, description, price_per_night, capacity, total_rooms)
VALUES
(2, 2, 'Deluxe Ocean Vista Suite',
 'Contemporary coastal 450 sq.ft suite with private sundeck balcony overlooking the Arabian Sea, premium king bedding, rain shower, and minibar.',
 5200.00, 2, 10);

INSERT IGNORE INTO room_type (room_type_id, hotel_id, room_name, description, price_per_night, capacity, total_rooms)
VALUES
(3, 3, 'Cedar Panorama Chalet',
 'Warm wooden interior crafted from Himalayan deodar cedar. Includes a private glass-front balcony with pine valley view, radiant room heating, and artisan coffee setup.',
 3800.00, 2, 6);

INSERT IGNORE INTO room_type (room_type_id, hotel_id, room_name, description, price_per_night, capacity, total_rooms)
VALUES
(4, 4, 'Plantation Vista Suite',
 'Elegant 480 sq.ft colonial-style suite with private wooden verandah opening straight onto emerald tea gardens, heritage cane furnishings, and luxury bathtub.',
 4400.00, 2, 5);

INSERT IGNORE INTO room_type (room_type_id, hotel_id, room_name, description, price_per_night, capacity, total_rooms)
VALUES
(5, 5, 'Lakeview Royal Deluxe',
 'Opulent Mewar-style deluxe room with direct unobstructed views of Lake Pichola, hand-painted ceiling motifs, plush marble bathroom, and evening turndown service.',
 7200.00, 2, 7);
