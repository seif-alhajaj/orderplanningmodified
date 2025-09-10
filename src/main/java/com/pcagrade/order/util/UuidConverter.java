// ============= UUIDCONVERTER - AUTOMATIC CONVERSION =============

// CREATE this new file: UuidConverter.java

package com.pcagrade.order.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
* Automatic UUID ↔ BINARY(16) converter
*
* Handles seamless conversion between Java UUID and MariaDB BINARY(16)
*/
@Converter(autoApply = true)
public class UuidConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        try {
            // Convert UUID to bytes for BINARY(16)
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());

            byte[] bytes = buffer.array();
            System.out.println(" UUID → BINARY(16): " + uuid + " → " + bytesToHex(bytes));

            return bytes;

        } catch (Exception e) {
            System.err.println(" Error conversion UUID → bytes: " + e.getMessage());
            return null;
        }
    }

    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        try {
           // Convert bytes BINARY(16) to UUID
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long mostSigBits = buffer.getLong();
            long leastSigBits = buffer.getLong();

            UUID uuid = new UUID(mostSigBits, leastSigBits);
            System.out.println(" BINARY(16) → UUID: " + bytesToHex(bytes) + " → " + uuid);

            return uuid;

        } catch (Exception e) {
            System.err.println(" Error conversion bytes → UUID: " + e.getMessage());
            return null;
        }
    }

    /**
    * Utility: Convert bytes to hex for debugging
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";

        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }
}