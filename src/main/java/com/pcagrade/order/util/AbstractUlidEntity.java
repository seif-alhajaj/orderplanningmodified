// ============= COMPLETE FIX - ABSTRACTULIDENTITY + SERVICES =============

// 1. REPLACE your AbstractUlidEntity.java COMPLETELY:

package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Abstract ULID entity stored as UUID in BINARY(16)
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractUlidEntity {

    @Id
    @GeneratedValue(generator = "ulid-generator")
    @GenericGenerator(
            name = "ulid-generator",
            strategy = "com.pcagrade.order.util.UlidGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;  // UUID instead of Ulid

    /**
     * JPA hook to ensure an ID is generated before persistence
     */
    @PrePersist
    protected void ensureId() {
        if (this.id == null) {
            // Generate a ULID and convert it to UUID
            Ulid ulid = UlidCreator.getUlid();
            this.id = ulid.toUuid();
            System.out.println("Generated ULID: " + ulid + " → UUID: " + this.id);
        }
    }

    /**
     * Gets the ID as a ULID String (conversion back from UUID)
     */
    @Transient
    public String getUlidString() {
        if (id == null) return null;
        try {
            // Convert UUID → ULID for display
            Ulid ulid = Ulid.from(id);
            return ulid.toString();
        } catch (Exception e) {
            // If not a valid ULID, return the UUID
            return id.toString();
        }
    }

    /**
     * Gets the ULID from the stored UUID
     */
    @Transient
    public Ulid getUlid() {
        if (id == null) return null;
        try {
            return Ulid.from(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the ID as hexadecimal (for native queries)
     */
    @Transient
    public String getIdAsHex() {
        if (id == null) return null;
        return id.toString().replace("-", "").toUpperCase();
    }

    /**
     * Sets the ID from a ULID string
     */
    public void setUlidFromString(String ulidString) {
        if (ulidString == null || ulidString.trim().isEmpty()) {
            this.id = null;
            return;
        }

        try {
            Ulid ulid = Ulid.from(ulidString.trim());
            this.id = ulid.toUuid();
        } catch (Exception e) {
            System.err.println("ULID string conversion error: " + e.getMessage());
            throw new IllegalArgumentException("Invalid ULID: " + ulidString, e);
        }
    }

    // Standard methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractUlidEntity that = (AbstractUlidEntity) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getUlidString() + "}";
    }
}

// ============================================================================