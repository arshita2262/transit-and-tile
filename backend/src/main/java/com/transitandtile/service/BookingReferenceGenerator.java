package com.transitandtile.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Generates a human-readable booking reference in the format TT-{DEST_CODE}-{RANDOM_4DIGIT}.
 *
 * Example: TT-GOA-4891, TT-JAI-2347
 *
 * The destination code is derived from the hotel location string.
 * The numeric suffix is a 4-digit random number padded with leading zeros if needed.
 * Callers should handle DataIntegrityViolationException and retry with a bounded count.
 */
@Component
public class BookingReferenceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Known location-to-code mappings for the seeded hotels.
     * Key: lowercased keywords found in location strings.
     * Value: 3-letter destination code.
     */
    private static final Map<String, String> LOCATION_CODE_MAP = Map.of(
        "jaipur",     "JAI",
        "goa",        "GOA",
        "manali",     "MNL",
        "munnar",     "MUN",
        "udaipur",    "UDR",
        "darjeeling", "DRJ",
        "ooty",       "OTY",
        "kerala",     "KER",
        "shimla",     "SML",
        "coorg",      "CRG"
    );

    private static final String FALLBACK_CODE = "TTL";

    /**
     * Generates a booking reference using the hotel location for the destination code.
     *
     * @param hotelLocation the hotel's location string (e.g., "Candolim, Goa")
     * @return a reference such as "TT-GOA-4891"
     */
    public String generate(String hotelLocation) {
        String destCode = resolveDestinationCode(hotelLocation);
        int suffix = 1000 + RANDOM.nextInt(9000); // 1000–9999
        return "TT-" + destCode + "-" + suffix;
    }

    private String resolveDestinationCode(String location) {
        if (location == null || location.isBlank()) {
            return FALLBACK_CODE;
        }
        String lower = location.toLowerCase();
        for (Map.Entry<String, String> entry : LOCATION_CODE_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Fallback: use first three letters of first word, uppercased
        String[] words = location.trim().split("[^a-zA-Z]+");
        for (String word : words) {
            if (word.length() >= 2) {
                return word.substring(0, Math.min(3, word.length())).toUpperCase();
            }
        }
        return FALLBACK_CODE;
    }
}
