package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity management
 * Translated from CommandeRepository to OrderRepository with enum support
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by order number
     * @param orderNumber the order number to search for
     * @return optional order
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find orders by status (using enum)
     * @param status the status enum to filter by
     * @return list of orders with specified status
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * Find orders by status (legacy support with ordinal)
     * @param statusOrdinal the status ordinal to filter by
     * @return list of orders with specified status
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findOrdersByStatus(@Param("status") Order.OrderStatus status);

    /**
     * Find unassigned orders (status = PENDING)
     * @param status the status enum (typically PENDING)
     * @return list of unassigned orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findUnassignedOrders(@Param("status") Order.OrderStatus status);

    /**
     * Count orders by status (using enum)
     * @param status the status to count
     * @return number of orders with this status
     */
    long countByStatus(Order.OrderStatus status);

    /**
     * Find orders that need processing (status PENDING or IN_PROGRESS)
     * @return list of orders to be processed, ordered by priority and date
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'IN_PROGRESS') ORDER BY o.priority DESC, o.orderDate ASC")
    List<Order> findOrdersToProcess();

    /**
     * Find orders created after specific date
     * @param date the date after which to search
     * @return list of orders created after the specified date
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate >= :date ORDER BY o.orderDate ASC")
    List<Order> findOrdersAfterDate(@Param("date") LocalDate date);

    /**
     * Find orders by priority
     * @param priority the priority level
     * @return list of orders with specified priority
     */
    List<Order> findByPriorityOrderByOrderDateAsc(Order.OrderPriority priority);

    /**
     * Find orders between dates
     * @param startDate start date
     * @param endDate end date
     * @return list of orders between specified dates
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate ASC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find pending orders
     * @return list of pending orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.priority DESC, o.orderDate ASC")
    List<Order> findPendingOrders();

    /**
     * Find in progress orders
     * @return list of in progress orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'IN_PROGRESS' ORDER BY o.orderDate ASC")
    List<Order> findInProgressOrders();

    /**
     * Find completed orders
     * @return list of completed orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.orderDate DESC")
    List<Order> findCompletedOrders();

    /**
     * Find orders by card count range
     * @param minCards minimum number of cards
     * @param maxCards maximum number of cards
     * @return list of orders within card count range
     */
    @Query("SELECT o FROM Order o WHERE o.cardCount BETWEEN :minCards AND :maxCards ORDER BY o.cardCount ASC")
    List<Order> findOrdersByCardCountRange(@Param("minCards") int minCards, @Param("maxCards") int maxCards);

    /**
     * Get total card count for all orders
     * @return sum of all cards across all orders
     */
    @Query("SELECT COALESCE(SUM(o.cardCount), 0) FROM Order o")
    Long getTotalCardCount();

    /**
     * Get average processing time in minutes
     * @return average processing time
     */
    @Query("SELECT AVG(o.estimatedTimeMinutes) FROM Order o WHERE o.estimatedTimeMinutes > 0")
    Double getAverageProcessingTime();

    /**
     * Find orders with deadlines approaching (within next N days)
     * @param targetDate the date to compare against
     * @return list of orders with approaching deadlines
     */
    @Query("SELECT o FROM Order o WHERE o.deadlineDate <= :targetDate AND o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.deadlineDate ASC")
    List<Order> findOrdersWithApproachingDeadlines(@Param("targetDate") LocalDateTime targetDate);

    /**
     * Find orders created between dates
     * @param startDate start date
     * @param endDate end date
     * @return list of orders created in the date range
     */
    @Query("SELECT o FROM Order o WHERE o.creationDate BETWEEN :startDate AND :endDate ORDER BY o.creationDate DESC")
    List<Order> findOrdersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find high priority orders
     * @return list of high priority orders
     */
    @Query("SELECT o FROM Order o WHERE o.priority = 'HIGH' AND o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.orderDate ASC")
    List<Order> findHighPriorityOrders();

    /**
     * Find overdue orders (deadline passed and not completed)
     * @return list of overdue orders
     */
    @Query("SELECT o FROM Order o WHERE o.deadlineDate < CURRENT_TIMESTAMP AND o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.deadlineDate ASC")
    List<Order> findOverdueOrders();

    /**
     * Count orders by priority
     * @param priority the priority to count
     * @return number of orders with this priority
     */
    long countByPriority(Order.OrderPriority priority);

    /**
     * Find orders by customer name (case insensitive)
     * @param customerName the customer name to search for
     * @return list of orders for the customer
     */
    @Query("SELECT o FROM Order o WHERE LOWER(o.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')) ORDER BY o.orderDate DESC")
    List<Order> findByCustomerNameContainingIgnoreCase(@Param("customerName") String customerName);

    /**
     * Find orders created in the last N days
     * @param days number of days to look back
     * @return list of recent orders
     */
    @Query("SELECT o FROM Order o WHERE o.creationDate >= :sinceDate ORDER BY o.creationDate DESC")
    List<Order> findRecentOrders(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find orders ready for scheduling (PENDING status)
     * @return list of orders ready to be scheduled
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.priority DESC, o.orderDate ASC")
    List<Order> findOrdersReadyForScheduling();

    /**
     * Get workload statistics (count and total estimated time by status)
     * @return list of object arrays containing [status, count, totalTime]
     */
    @Query("SELECT o.status, COUNT(o), COALESCE(SUM(o.estimatedTimeMinutes), 0) FROM Order o GROUP BY o.status")
    List<Object[]> getWorkloadByStatus();

    /**
     * Get orders summary by priority
     * @return list of object arrays containing [priority, count, totalCards]
     */
    @Query("SELECT o.priority, COUNT(o), COALESCE(SUM(o.cardCount), 0) FROM Order o GROUP BY o.priority ORDER BY o.priority")
    List<Object[]> getOrdersSummaryByPriority();
}