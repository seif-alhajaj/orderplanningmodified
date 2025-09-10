package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;

import java.util.UUID;

/**
*  Utilities for ULID ↔ UUID conversion
*/
public class UlidHelper {

   /**
     * Converts a String ID to UUID (handles ULID, UUID, hex)
     */
    public static UUID stringToUuid(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            return null;
        }

        idString = idString.trim();

        try {
            // Format ULID standard (26 characters)
            if (idString.length() == 26 && idString.matches("[0-9A-Z]+")) {
                Ulid ulid = Ulid.from(idString);
                return ulid.toUuid();
            }

            // Format UUID with hyphens(36 characters)
            if (idString.length() == 36 && idString.contains("-")) {
                return UUID.fromString(idString);
            }

            // Hex format without hyphens (32 characters)
            if (idString.length() == 32 && idString.matches("[0-9A-Fa-f]+")) {
                String formatted = idString.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            throw new IllegalArgumentException("Unrecognized ID format: " + idString);

        } catch (Exception e) {
            System.err.println(" Error conversion ID: " + idString + " - " + e.getMessage());
            throw new IllegalArgumentException("ID invalide: " + idString, e);
        }
    }

    /**
     * Converts a UUID to a String ULID for display
     */
    public static String uuidToUlidString(UUID uuid) {
        if (uuid == null) return null;
        try {
            Ulid ulid = Ulid.from(uuid);
            return ulid.toString();
        } catch (Exception e) {
            return uuid.toString();
        }
    }
}
