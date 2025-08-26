package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Planning entity management
 * Translated from PlanificationRepository to PlanningRepository with all required methods
 */
@Repository
public interface PlanningRepository extends JpaRepository<Planning, UUID> {

    /**
     * Find planning entries by employee ID ordered by start time
     * @param employeeId the employee ID
     * @return list of planning entries sorted by start time
     */
    List<Planning> findByEmployeeIdOrderByStartTimeAsc(UUID employeeId);

    /**
     * Find planning entries by employee ID (string) ordered by start time
     * @param employeeId the employee ID as string
     * @return list of planning entries sorted by start time
     */
    @Query("SELECT p FROM Planning p WHERE CAST(p.employeeId AS string) = :employeeId ORDER BY p.startTime ASC")
    List<Planning> findByEmployeeIdOrderByStartTimeAsc(String employeeId);

    /**
     * Find planning entries by order ID ordered by start time
     * @param orderId the order ID
     * @return list of planning entries sorted by start time
     */
    List<Planning> findByOrderIdOrderByStartTimeAsc(UUID orderId);

    /**
     * Find planning entries by order ID (string) ordered by start time
     * @param orderId the order ID as string
     * @return list of planning entries sorted by start time
     */
    @Query("SELECT p FROM Planning p WHERE CAST(p.orderId AS string) = :orderId ORDER BY p.startTime ASC")
    List<Planning> findByOrderIdOrderByStartTimeAsc(String orderId);

    /**
     * Find planning entries by employee ID and date range
     * @param employeeId the employee ID
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of planning entries sorted by date and start time
     */
    @Query("SELECT p FROM Planning p WHERE p.employeeId = :employeeId " +
            "AND p.planningDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.planningDate, p.startTime")
    List<Planning> findByEmployeeIdAndPlanningDateBetween(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find planning entries by date range
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of planning entries sorted by date and start time
     */
    @Query("SELECT p FROM Planning p WHERE p.planningDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.planningDate, p.startTime")
    List<Planning> findByPlanningDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find planning entries by start time between two LocalDateTime values
     * @param startTime start time
     * @param endTime end time
     * @return list of planning entries sorted by start time
     */
    List<Planning> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find planning entries by order ID
     * @param orderId the order ID
     * @return list of planning entries for the specified order
     */
    @Query("SELECT p FROM Planning p WHERE p.orderId = :orderId ORDER BY p.planningDate, p.startTime")
    List<Planning> findByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find incomplete planning entries (not completed)
     * @return list of incomplete planning entries
     */
    @Query("SELECT p FROM Planning p WHERE p.status != 'COMPLETED' " +
            "ORDER BY p.planningDate, p.startTime")
    List<Planning> findIncompleteEntries();

    /**
     * Find planning entries by status
     * @param status the planning status
     * @return list of planning entries with specified status
     */
    List<Planning> findByStatus(Planning.PlanningStatus status);

    /**
     * Count planning entries by status
     * @param status the planning status
     * @return number of planning entries with specified status
     */
    long countByStatus(Planning.PlanningStatus status);

    /**
     * Find planning entries by priority
     * @param priority the planning priority
     * @return list of planning entries with specified priority
     */
    List<Planning> findByPriority(Planning.PlanningPriority priority);

    /**
     * Count planning entries by priority
     * @param priority the planning priority
     * @return number of planning entries with specified priority
     */
    long countByPriority(Planning.PlanningPriority priority);

    /**
     * Count planning entries created after a specific date
     * @param createdAfter the date threshold
     * @return number of planning entries created after the date
     */
    long countByCreatedAtAfter(LocalDateTime createdAfter);

    /**
     * Find employee workload distribution
     * @return list of objects containing employee ID and workload data
     */
    @Query("SELECT p.employeeId, COUNT(p), SUM(p.estimatedDurationMinutes) " +
            "FROM Planning p " +
            "WHERE p.status IN ('SCHEDULED', 'IN_PROGRESS') " +
            "GROUP BY p.employeeId " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> findEmployeeWorkloadDistribution();

    /**
     * Find planning entries for a specific date
     * @param planningDate the planning date
     * @return list of planning entries for the specified date
     */
    List<Planning> findByPlanningDate(LocalDate planningDate);

    /**
     * Find planning entries by employee and date
     * @param employeeId the employee ID
     * @param planningDate the planning date
     * @return list of planning entries for the employee on the specified date
     */
    List<Planning> findByEmployeeIdAndPlanningDate(UUID employeeId, LocalDate planningDate);

    /**
     * Find overlapping planning entries for an employee
     * @param employeeId the employee ID
     * @param startTime start time to check
     * @param endTime end time to check
     * @return list of overlapping planning entries
     */
    @Query("SELECT p FROM Planning p WHERE p.employeeId = :employeeId " +
            "AND p.startTime < :endTime AND p.endTime > :startTime")
    List<Planning> findOverlappingEntries(
            @Param("employeeId") UUID employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get planning statistics for a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of planning statistics
     */
    @Query("SELECT p.status, COUNT(p), AVG(p.estimatedDurationMinutes) " +
            "FROM Planning p " +
            "WHERE p.planningDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.status")
    List<Object[]> getPlanningStatistics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find planning entries that are overdue
     * @param currentTime current timestamp
     * @return list of overdue planning entries
     */
    @Query("SELECT p FROM Planning p WHERE p.endTime < :currentTime " +
            "AND p.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "ORDER BY p.endTime ASC")
    List<Planning> findOverdueEntries(@Param("currentTime") LocalDateTime currentTime);
}