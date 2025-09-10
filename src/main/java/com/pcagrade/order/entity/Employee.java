// ========== ENGLISH VERSION: Employee.java ==========
// src/main/java/com/pcagrade/order/entity/Employee.java

package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee Entity - English Version
 * Represents an employee in the Pokemon card processing system
 */
@Entity
@Table(name = "j_employee")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AbstractUlidEntity {

    /**
     * Employee's first name
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * Employee's last name
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * Employee's email address
     */
    @Column(name = "email", length = 150)
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Employee's role (GRADER, CERTIFIER, etc.)
     */
    @Column(name = "role", length = 20)
    @Builder.Default
    private String role = "GRADER";  // ← ADDED ROLE FIELD

    /**
     * Work hours per day (default: 8 hours)
     */
    @Column(name = "work_hours_per_day")
    @Positive(message = "Work hours per day must be positive")
    @Builder.Default
    private Integer workHoursPerDay = 8;

    /**
     * Whether the employee is active
     */
    @Column(name = "active")
    @NotNull
    @Builder.Default
    private Boolean active = true;

    /**
     * Date when the employee was created
     */
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    /**
     * Date when the employee was last modified
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ========== RELATIONSHIPS ==========

    /**
     * Planning assignments for this employee
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Planning> plannings;

    // ========== BUSINESS METHODS ==========

    /**
     * Get employee's full name
     * @return firstName + " " + lastName
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get employee's initials
     * @return first letter of firstName + first letter of lastName
     */
    public String getInitials() {
        if (firstName != null && lastName != null &&
                !firstName.isEmpty() && !lastName.isEmpty()) {
            return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
        }
        return "??";
    }

    /**
     * Calculate maximum work minutes per day
     * @return workHoursPerDay * 60
     */
    public Integer getMaxWorkMinutesPerDay() {
        return workHoursPerDay != null ? workHoursPerDay * 60 : 480; // Default 8h = 480min
    }

    /**
     * Check if employee is available for work
     * @return true if active
     */
    public boolean isAvailable() {
        return active != null && active;
    }

    /**
     * Check if employee is a certifier
     * @return true if role is CERTIFIER
     */
    public boolean isCertifier() {
        return "CERTIFIER".equalsIgnoreCase(role);
    }

    /**
     * Check if employee is a grader
     * @return true if role is GRADER
     */
    public boolean isGrader() {
        return "GRADER".equalsIgnoreCase(role) || role == null;
    }

    // ========== LIFECYCLE CALLBACKS ==========

    /**
     * Set creation date before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
        if (modificationDate == null) {
            modificationDate = LocalDateTime.now();
        }
        // Ensure role has a default value
        if (role == null) {
            role = "GRADER";
        }
    }

    /**
     * Update modification date before updating
     */
    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();
    }

    // ========== BUILDER CUSTOMIZATION ==========

    /**
     * Custom builder method to create an active employee with current timestamps
     */
    public static Employee createActiveEmployee(String firstName, String lastName, String email, Integer workHours) {
        return Employee.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .workHoursPerDay(workHours != null ? workHours : 8)
                .active(true)
                .creationDate(LocalDateTime.now())
                .modificationDate(LocalDateTime.now())
                .build();
    }

    /**
     * Custom builder method to create an employee with specific role
     */
    public static Employee createEmployeeWithRole(String firstName, String lastName, String email, String role, Integer workHours) {
        return Employee.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .role(role != null ? role : "GRADER")
                .workHoursPerDay(workHours != null ? workHours : 8)
                .active(true)
                .creationDate(LocalDateTime.now())
                .modificationDate(LocalDateTime.now())
                .build();
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate employee data
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                workHoursPerDay != null && workHoursPerDay > 0 &&
                active != null;
    }

    /**
     * Get validation errors
     * @return list of validation error messages
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (firstName == null || firstName.trim().isEmpty()) {
            errors.add("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.add("Last name is required");
        }
        if (email != null && !email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errors.add("Email format is invalid");
        }
        if (workHoursPerDay == null || workHoursPerDay <= 0) {
            errors.add("Work hours per day must be positive");
        }
        if (workHoursPerDay != null && workHoursPerDay > 12) {
            errors.add("Work hours per day cannot exceed 12 hours");
        }
        if (role != null && !role.matches("^(GRADER|CERTIFIER)$")) {
            errors.add("Role must be either GRADER or CERTIFIER");
        }

        return errors;
    }

    public Boolean getActive() { return active; }
    public Boolean getIsActive() { return active; }
    public boolean isActive() { return active != null && active; }
}