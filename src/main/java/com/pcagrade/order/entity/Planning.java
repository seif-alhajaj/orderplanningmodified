package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Planning entity for managing work schedules and task assignments
 * Translated from Planification to Planning with correct method names
 */
@Entity
@Table(name = "j_planning",
        indexes = {
                @Index(name = "idx_planning_employee_date", columnList = "employeeId, planningDate"),
                @Index(name = "idx_planning_order", columnList = "orderId"),
                @Index(name = "idx_planning_date", columnList = "planningDate"),
                @Index(name = "idx_planning_status", columnList = "status"),
                @Index(name = "idx_planning_start_time", columnList = "startTime"),
                @Index(name = "idx_planning_employee_time", columnList = "employeeId, planningDate, startTime")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Planning extends AbstractUlidEntity {

    /**
     * ID of the order this planning entry is for
     */
    @NotNull(message = "Order ID is required")
    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderId;

    /**
     * ID of the employee assigned to this task
     */
    @NotNull(message = "Employee ID is required")
    @Column(name = "employee_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID employeeId;

    /**
     * Date when this task is planned
     */
    @NotNull(message = "Planning date is required")
    @Column(name = "planning_date", nullable = false)
    private LocalDate planningDate;

    /**
     * Start time of the task
     */
    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime; // Changed to LocalDateTime for compatibility

    /**
     * End time of the task
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Estimated duration of the task in minutes
     */
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 720, message = "Duration cannot exceed 12 hours (720 minutes)")
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    /**
     * Estimated end time (calculated from start time + duration)
     */
    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    /**
     * Priority level of this planning entry
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "priority", length = 20)
    private PlanningPriority priority = PlanningPriority.FAST;

    /**
     * Status of this planning entry
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20)
    private PlanningStatus status = PlanningStatus.SCHEDULED;

    /**
     * Actual start time (when task actually began)
     */
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    /**
     * Actual end time (when task actually finished)
     */
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    /**
     * Progress percentage (0-100)
     */
    @Min(value = 0, message = "Progress cannot be negative")
    @Max(value = 100, message = "Progress cannot exceed 100%")
    @Builder.Default
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    /**
     * Number of cards to process in this planning entry
     */
    @Min(value = 0, message = "Card count cannot be negative")
    @Column(name = "card_count")
    private Integer cardCount;

    /**
     * Additional notes or comments
     */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Estimated cost for this planning entry (optional)
     */
    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid cost format")
    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    /**
     * Actual cost for this planning entry (optional)
     */
    @DecimalMin(value = "0.0", message = "Actual cost cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid actual cost format")
    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    /**
     * Creation timestamp
     */
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last update timestamp
     */
    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Whether this task has been completed (legacy field for compatibility)
     */
    @Column(name = "completed")
    private Boolean completed = false;

    // ========== RELATIONSHIPS ==========

    /**
     * Reference to the Order entity (lazy loaded to avoid performance issues)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    /**
     * Reference to the Employee entity (lazy loaded to avoid performance issues)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    // ========== ENUMS ==========

    /**
     * Priority levels for planning entries
     */
    public enum PlanningPriority {
        EXCELSIOR,    // Must be done immediately
        FAST_PLUS,      // Important, should be done soon
        FAST,    // Normal priority
        CLASSIC        // Can be delayed if needed
    }

    /**
     * Status of planning entries
     */
    public enum PlanningStatus {
        SCHEDULED,   // Planned but not started
        IN_PROGRESS, // Currently being worked on
        PAUSED,      // Temporarily stopped
        COMPLETED,   // Finished successfully
        CANCELLED,   // Cancelled before completion
        OVERDUE      // Past due date and not completed
    }

    // ========== CONSTRUCTORS ==========

    /**
     * Constructor for basic planning entry
     */
    public Planning(UUID orderId, UUID employeeId, LocalDate planningDate,
                    LocalDateTime startTime, Integer estimatedDurationMinutes) {
        this.orderId = orderId;
        this.employeeId = employeeId;
        this.planningDate = planningDate;
        this.startTime = startTime;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.status = PlanningStatus.SCHEDULED;
        this.priority = PlanningPriority.FAST;
        this.progressPercentage = 0;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Calculate estimated end time
        if (startTime != null && estimatedDurationMinutes != null) {
            this.estimatedEndTime = startTime.plusMinutes(estimatedDurationMinutes);
        }
    }

    // ========== LIFECYCLE HOOKS ==========

    /**
     * Set creation date before persist
     */
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PlanningStatus.SCHEDULED;
        }
        if (this.priority == null) {
            this.priority = PlanningPriority.FAST;
        }
        if (this.progressPercentage == null) {
            this.progressPercentage = 0;
        }
        if (this.completed == null) {
            this.completed = false;
        }

        // Calculate estimated end time if not set
        if (this.estimatedEndTime == null && this.startTime != null && this.estimatedDurationMinutes != null) {
            this.estimatedEndTime = this.startTime.plusMinutes(this.estimatedDurationMinutes);
        }
    }

    /**
     * Update modification date before update
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        // Update completed status based on status
        if (this.status == PlanningStatus.COMPLETED) {
            this.completed = true;
            this.progressPercentage = 100;
            if (this.actualEndTime == null) {
                this.actualEndTime = LocalDateTime.now();
            }
        } else {
            this.completed = false;
        }

        // Recalculate estimated end time if changed
        if (this.startTime != null && this.estimatedDurationMinutes != null) {
            this.estimatedEndTime = this.startTime.plusMinutes(this.estimatedDurationMinutes);
        }
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Calculate estimated end time based on start time and duration
     * @return estimated end time
     */
    public LocalDateTime calculateEstimatedEndTime() {
        if (startTime != null && estimatedDurationMinutes != null) {
            return startTime.plusMinutes(estimatedDurationMinutes);
        }
        return null;
    }

    /**
     * Get actual duration in minutes (if task is completed)
     * @return actual duration or null if not completed
     */
    public Integer getActualDurationMinutes() {
        if (actualStartTime != null && actualEndTime != null) {
            return (int) java.time.Duration.between(actualStartTime, actualEndTime).toMinutes();
        }
        return null;
    }

    /**
     * Check if this planning entry is overdue
     * @return true if past the estimated end time and not completed
     */
    public boolean isOverdue() {
        return estimatedEndTime != null
                && estimatedEndTime.isBefore(LocalDateTime.now())
                && status != PlanningStatus.COMPLETED
                && status != PlanningStatus.CANCELLED;
    }

    /**
     * Check if this planning entry is for today
     * @return true if planned for today
     */
    public boolean isToday() {
        return planningDate != null && planningDate.equals(LocalDate.now());
    }

    /**
     * Check if this planning entry is in the future
     * @return true if planned for future date
     */
    public boolean isFuture() {
        return planningDate != null && planningDate.isAfter(LocalDate.now());
    }

    /**
     * Get formatted time range (e.g., "09:00 - 10:30")
     * @return formatted time range
     */
    public String getTimeRange() {
        if (startTime != null && estimatedEndTime != null) {
            return String.format("%02d:%02d - %02d:%02d",
                    startTime.getHour(), startTime.getMinute(),
                    estimatedEndTime.getHour(), estimatedEndTime.getMinute());
        } else if (startTime != null && estimatedDurationMinutes != null) {
            LocalDateTime endTime = startTime.plusMinutes(estimatedDurationMinutes);
            return String.format("%02d:%02d - %02d:%02d",
                    startTime.getHour(), startTime.getMinute(),
                    endTime.getHour(), endTime.getMinute());
        }
        return "Time not set";
    }

    /**
     * Get remaining time in minutes
     * @return remaining time or null if not applicable
     */
    public Integer getRemainingTimeMinutes() {
        if (estimatedEndTime != null && status == PlanningStatus.IN_PROGRESS) {
            long remaining = java.time.Duration.between(LocalDateTime.now(), estimatedEndTime).toMinutes();
            return remaining > 0 ? (int) remaining : 0;
        }
        return null;
    }

    /**
     * Mark as started
     */
    public void markAsStarted() {
        this.status = PlanningStatus.IN_PROGRESS;
        this.actualStartTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as completed
     */
    public void markAsCompleted() {
        this.status = PlanningStatus.COMPLETED;
        this.completed = true;
        this.progressPercentage = 100;
        this.actualEndTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as cancelled
     */
    public void markAsCancelled() {
        this.status = PlanningStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update progress
     * @param progressPercentage the new progress percentage (0-100)
     */
    public void updateProgress(int progressPercentage) {
        if (progressPercentage < 0 || progressPercentage > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        this.progressPercentage = progressPercentage;
        this.updatedAt = LocalDateTime.now();

        if (progressPercentage == 100 && this.status != PlanningStatus.COMPLETED) {
            markAsCompleted();
        }
    }


}