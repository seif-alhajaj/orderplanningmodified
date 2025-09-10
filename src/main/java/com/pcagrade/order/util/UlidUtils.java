package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;

public class UlidUtils {

    /**
     * Converts a hex string to Ulid
     */
    public static Ulid fromHexString(String hexString) {
        if (hexString == null || hexString.length() != 32) {
            throw new IllegalArgumentException("Hex string doit faire 32 caractères");
        }

       // Convert hex string to bytes
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            bytes[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }

        return Ulid.from(bytes);
    }

   /**
     * Converts an Ulid to hex string
     */
    public static String toHexString(Ulid ulid) {
        byte[] bytes = ulid.toBytes();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
