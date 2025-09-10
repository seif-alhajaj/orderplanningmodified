// ============= ULIDGENERATOR - GENERATE UUID FROM ULID =============

//  REPLACE your UlidGenerator.java with this version:

package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 *  ULID generator compatible with BINARY(16)
 *
 * Generates ULIDs and converts them to UUID for MariaDB storage
 * ULIDs maintain chronological order even when converted to UUID
 */
public class UlidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        try {
            // Generate a monotonic ULID (chronological order guaranteed)
            Ulid ulid = UlidCreator.getMonotonicUlid();

            // Convert to UUID for BINARY(16) storage
            UUID uuid = ulid.toUuid();

            System.out.println("Generated ULID: " + ulid + " -> UUID: " + uuid);

            return uuid; //  Return UUID for BINARY(16) compatibility

        } catch (Exception e) {
            System.err.println("ULID generation error: " + e.getMessage());
            // Fallback: classic UUID
            UUID fallback = UUID.randomUUID();
            System.out.println("Fallback UUID: " + fallback);
            return fallback;
        }
    }
}