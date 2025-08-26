package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Order entity representing Pokemon card orders
 * Translated from Commande to Order with enhanced functionality
 */
@Entity
@Table(name = "`order`")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AbstractUlidEntity {

    /**
     * Unique order number for tracking
     */
    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number must not exceed 50 characters")
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Number of cards in this order
     */
    @NotNull(message = "Card count is required")
    @Positive(message = "Card count must be positive")
    @Min(value = 1, message = "Minimum 1 card per order")
    @Max(value = 10000, message = "Maximum 10000 cards per order")
    @Column(name = "card_count", nullable = false)
    private Integer cardCount;

    /**
     * Order priority level
     */
    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private OrderPriority priority = OrderPriority.MEDIUM;

    /**
     * Total price of the order (removed precision and scale for Double)
     */
    @DecimalMin(value = "0.0", message = "Total price cannot be negative")
    @Column(name = "total_price")
    private Double totalPrice;

    /**
     * Estimated processing time in minutes
     */
    @Min(value = 1, message = "Estimated time must be at least 1 minute")
    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    /**
     * Customer name or identifier
     */
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Order status
     */
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Date when the order was placed
     */
    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();

    /**
     * Deadline for completing this order
     */
    @Column(name = "deadline_date")
    private LocalDateTime deadlineDate;

    /**
     * When processing started
     */
    @Column(name = "processing_start_date")
    private LocalDateTime processingStartDate;

    /**
     * When processing ended
     */
    @Column(name = "processing_end_date")
    private LocalDateTime processingEndDate;

    /**
     * Additional notes or special instructions
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Record creation timestamp
     */
    @Column(name = "creation_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now();

    /**
     * Record last modification timestamp
     */
    @Column(name = "modification_date", nullable = false)
    @Builder.Default
    private LocalDateTime modificationDate = LocalDateTime.now();

    // ========== ENUMS ==========

    /**
     * Order priority levels
     */
    public enum OrderPriority {
        HIGH,     // 1 week - price >= 1000€
        MEDIUM,   // 2 weeks - price >= 500€
        LOW       // 4 weeks - price < 500€
    }

    /**
     * Order status enumeration
     */
    public enum OrderStatus {
        PENDING,      // Waiting to be processed
        SCHEDULED,    // Scheduled for processing
        IN_PROGRESS,  // Currently being processed
        COMPLETED,    // Processing completed
        CANCELLED     // Order cancelled
    }

    // ========== LIFECYCLE HOOKS ==========

    /**
     * Set creation date before persist
     */
    @PrePersist
    protected void onCreate() {
        if (this.creationDate == null) {
            this.creationDate = LocalDateTime.now();
        }
        if (this.modificationDate == null) {
            this.modificationDate = LocalDateTime.now();
        }
        if (this.orderDate == null) {
            this.orderDate = LocalDate.now();
        }
        if (this.priority == null) {
            this.priority = OrderPriority.MEDIUM;
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
        // Calculate estimated time if not set
        if (this.estimatedTimeMinutes == null && this.cardCount != null) {
            calculateEstimatedTime();
        }
    }

    /**
     * Update modification date before update
     */
    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = LocalDateTime.now();
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Calculate estimated processing time based on card count
     * Default: 3 minutes per card
     */
    public void calculateEstimatedTime() {
        if (this.cardCount != null) {
            this.estimatedTimeMinutes = this.cardCount * 3; // 3 minutes per card
        }
    }

    /**
     * Check if order is overdue
     */
    public boolean isOverdue() {
        return deadlineDate != null
                && deadlineDate.isBefore(LocalDateTime.now())
                && status != OrderStatus.COMPLETED
                && status != OrderStatus.CANCELLED;
    }

    /**
     * Check if order is high priority
     */
    public boolean isHighPriority() {
        return priority == OrderPriority.HIGH;
    }

    /**
     * Check if order is ready for processing
     */
    public boolean isReadyForProcessing() {
        return status == OrderStatus.PENDING || status == OrderStatus.SCHEDULED;
    }

    /**
     * Check if order is in progress
     */
    public boolean isInProgress() {
        return status == OrderStatus.IN_PROGRESS;
    }

    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    /**
     * Mark order as started
     */
    public void markAsStarted() {
        this.status = OrderStatus.IN_PROGRESS;
        this.processingStartDate = LocalDateTime.now();
        this.modificationDate = LocalDateTime.now();
    }

    /**
     * Mark order as completed
     */
    public void markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.processingEndDate = LocalDateTime.now();
        this.modificationDate = LocalDateTime.now();
    }


    /**
     * Get formatted priority display
     */
    public String getPriorityDisplay() {
        return switch (priority) {
            case HIGH -> "Haute priorité";
            case MEDIUM -> "Priorité normale";
            case LOW -> "Basse priorité";
        };
    }

    /**
     * Get formatted status display
     */
    public String getStatusDisplay() {
        return switch (status) {
            case PENDING -> "En attente";
            case SCHEDULED -> "Planifiée";
            case IN_PROGRESS -> "En cours";
            case COMPLETED -> "Terminée";
            case CANCELLED -> "Annulée";
        };
    }

    /**
     * Calculate estimated end date based on start date and processing time
     */
    public LocalDateTime getEstimatedEndDate() {
        if (processingStartDate != null && estimatedTimeMinutes != null) {
            return processingStartDate.plusMinutes(estimatedTimeMinutes);
        }
        return null;
    }

    /**
     * Get actual processing duration in minutes
     */
    public Integer getActualProcessingTimeMinutes() {
        if (processingStartDate != null && processingEndDate != null) {
            return (int) java.time.Duration.between(processingStartDate, processingEndDate).toMinutes();
        }
        return null;
    }

    /**
     * Check if processing is delayed
     */
    public boolean isProcessingDelayed() {
        if (processingStartDate != null && estimatedTimeMinutes != null && status == OrderStatus.IN_PROGRESS) {
            LocalDateTime estimatedEnd = processingStartDate.plusMinutes(estimatedTimeMinutes);
            return LocalDateTime.now().isAfter(estimatedEnd);
        }
        return false;
    }
}