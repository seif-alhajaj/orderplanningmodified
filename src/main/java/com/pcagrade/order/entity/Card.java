package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

/**
 * CARD ENTITY - SHARED TABLE WITH OTHER PROJECTS
 *
 * IMPORTANT: This table is used by multiple projects.
 * - DO NOT perform automatic migrations
 * - DO NOT modify the existing structure
 * - Use only for READ operations in this planning project
 */
@Getter
@Setter
@Entity
@Table(name = "card")
public class Card extends AbstractUlidEntity {

    @Size(max = 255)
    @NotNull
    @Column(name = "discriminator", nullable = false)
    private String discriminator;

    @Size(max = 255)
    @NotNull
    @Column(name = "num", nullable = false)
    private String num;

    /**
     * CORRECTION: Mapping of existing column WITHOUT migration
     * - Use @Basic instead of @Lob to avoid conversion attempts
     * - No columnDefinition to avoid conflicts
     * - Hibernate will use the existing database definition
     */
    @NotNull
    @Basic(fetch = FetchType.LAZY) // Lazy loading for large attributes
    @Column(name = "attributes", nullable = false)
    private String attributes;

    /**
     * CORRECTION: Mapping of existing notes WITHOUT migration
     */
    @NotNull
    @ColumnDefault("'[]'")
    @Basic(fetch = FetchType.LAZY) // Lazy loading
    @Column(name = "allowed_notes", nullable = false)
    private String allowedNotes;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "ap", nullable = false)
    private Boolean ap = false;

    @Column(name = "image_id")
    private Integer imageId;

    /**
     * Relation with translations - READ ONLY for safety
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "card") // No cascade to avoid modifications
    private List<CardTranslation> translations = new ArrayList<>();

    // ===============================================
    // UTILITY METHODS FOR PLANNING
    // ===============================================

    /**
     * Gets the card name for planning (read only)
     */
    public String getNomPourPlanification() {
        if (translations != null && !translations.isEmpty()) {
            return translations.get(0).getName();
        }
        return "Card " + discriminator + " " + num;
    }

    /**
     * Checks if the card has attributes (for time estimation)
     */
    public boolean hasComplexAttributes() {
        return attributes != null && attributes.length() > 100;
    }

    /**
     * Processing time estimation based on complexity
     */
    public int getEstimatedProcessingMinutes() {
        int baseTime = 3; // 3 minutes default

        if (hasComplexAttributes()) {
            baseTime += 2; // +2 minutes for complex cards
        }

        if (translations != null && translations.size() > 1) {
            baseTime += 1; // +1 minute for multilingual cards
        }

        return baseTime;
    }
}