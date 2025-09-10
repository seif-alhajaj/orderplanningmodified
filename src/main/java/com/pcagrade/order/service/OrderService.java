package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

//========== MISSING IMPORTS TO FIX ERRORS ==========
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Validated
@Slf4j
public class OrderService {

    private static final int DEFAULT_PROCESSING_TIME_PER_CARD = 3; // minutes per card
    private static final int MAX_CARDS_PER_ORDER = 1000;
    private static final int MIN_CARDS_PER_ORDER = 1;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new order
     * @param order the order to create
     * @return created order
     */
    public Order createOrder(@Valid @NotNull Order order) {
        log.info("Creating new order: {}", order.getOrderNumber());

        // Validate business rules
        validateNewOrder(order);

        // Set default values
        if (order.getStatus() == null) {
            order.setStatus(Order.OrderStatus.PENDING);
        }
        if (order.getEstimatedTimeMinutes() == null && order.getCardCount() != null) {
            order.setEstimatedTimeMinutes(calculateEstimatedTime(order.getCardCount()));
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (order.getPriority() == null) {
            order.setPriority(Order.OrderPriority.FAST);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    /**
     * Update an existing order
     * @param order the order to update
     * @return updated order
     */
    public Order updateOrder(@Valid @NotNull Order order) {
        log.info("Updating order: {}", order.getId());

        if (!orderRepository.existsById(order.getId())) {
            throw new IllegalArgumentException("Order not found with ID: " + order.getId());
        }

        // Recalculate estimated time if card count changed
        if (order.getCardCount() != null && order.getEstimatedTimeMinutes() == null) {
            order.setEstimatedTimeMinutes(calculateEstimatedTime(order.getCardCount()));
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully: {}", updatedOrder.getId());
        return updatedOrder;
    }

    /**
     * Find order by ID
     * @param id the order ID
     * @return optional order
     */
    @Transactional(readOnly = true)
    public Optional<Order> findById(@NotNull UUID id) {
        return orderRepository.findById(id);
    }

    /**
     * Find order by order number
     * @param orderNumber the order number
     * @return optional order
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(@NotNull String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Get all orders with pagination
     * @param pageable pagination information
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * Delete an order
     * @param id the order ID
     */
    public void deleteOrder(@NotNull UUID id) {
        log.info("Deleting order: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with ID: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Order deleted successfully: {}", id);
    }

    // ========== ORDER STATUS OPERATIONS ==========

    /**
     * Find orders by status
     * @param status the order status
     * @return list of orders
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersByStatus(@NotNull Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findOrdersByStatusAsMap(@NotNull Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream().map(this::convertOrderToMap).collect(Collectors.toList());
    }

    private Map<String, Object> convertOrderToMap(Order order) {
        Map<String, Object> orderMap = new HashMap<>();
        if (order != null) {
            orderMap.put("id", order.getId() != null ? order.getId().toString() : null);
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("cardCount", order.getCardCount());
            orderMap.put("estimatedTimeMinutes", order.getEstimatedTimeMinutes());
            orderMap.put("priority", order.getPriority() != null ? order.getPriority().name() : null);
            orderMap.put("status", order.getStatus() != null ? order.getStatus().name() : null);
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("totalPrice", order.getTotalPrice());
        }
        return orderMap;
    }
    @Transactional(readOnly = true)
    public List<Order> findOrdersByStatusCorrect(@NotNull Order.OrderStatus status) {
        // ✅ Cette méthode retourne List<Order> et non List<Map<String, Object>>
        return orderRepository.findByStatus(status);
    }
    /**
     * Find pending orders (status = PENDING)
     * @return list of pending orders
     */
    @Transactional(readOnly = true)
    public List<Order> findPendingOrders() {
        return orderRepository.findUnassignedOrders(Order.OrderStatus.PENDING);
    }

    /**
     * Find in progress orders (status = IN_PROGRESS)
     * @return list of in progress orders
     */
    @Transactional(readOnly = true)
    public List<Order> findInProgressOrders() {
        return orderRepository.findByStatus(Order.OrderStatus.IN_PROGRESS);
    }

    /**
     * Find completed orders (status = COMPLETED)
     * @return list of completed orders
     */
    @Transactional(readOnly = true)
    public List<Order> findCompletedOrders() {
        return orderRepository.findByStatus(Order.OrderStatus.COMPLETED);
    }

    /**
     * Update order status
     * @param orderId the order ID
     * @param newStatus the new status
     * @return updated order
     */
    public Order updateOrderStatus(@NotNull UUID orderId, @NotNull Order.OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        // Set processing dates based on status
        if (newStatus == Order.OrderStatus.IN_PROGRESS && order.getProcessingStartDate() == null) {
            order.setProcessingStartDate(LocalDateTime.now());
        } else if (newStatus == Order.OrderStatus.COMPLETED && order.getProcessingEndDate() == null) {
            order.setProcessingEndDate(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully: {} -> {}", orderId, newStatus);
        return updatedOrder;
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Calculate estimated processing time
     * @param cardCount number of cards
     * @return estimated time in minutes
     */
    public int calculateEstimatedTime(@Positive int cardCount) {
        return cardCount * DEFAULT_PROCESSING_TIME_PER_CARD;
    }

    /**
     * Get orders that need planning from a specific date - FIXED VERSION WITHOUT DUPLICATES
     * Excludes orders that are already planned in j_planning table
     * @param day day of the month
     * @param month month (1-12)
     * @param year year
     * @return list of orders as maps for compatibility (excluding already planned)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdersForPlanning(int day, int month, int year) {
        try {
            log.info(" Loading orders for planning since {}/{}/{} (excluding already planned)", day, month, year);

            String fromDate = String.format("%04d-%02d-%02d", year, month, day);

            String sql = """
            SELECT DISTINCT
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                o.date as orderDate,
                COALESCE(o.delai, 'FAST') as deadline,
                COALESCE(
                    (SELECT COUNT(*) FROM card_certification_order cco 
                     WHERE cco.order_id = o.id), 10
                ) as cardCount,
                CASE
                   WHEN o.delai = 'X' THEN 'Excelsior'
                   WHEN o.delai = 'F+' THEN 'Fast+'
                   WHEN o.delai = 'F' THEN 'Fast'
                   WHEN o.delai = 'C' THEN 'Classic'
                   ELSE 'Classic'
                END as priority,
                o.status,
                o.prix_total as totalPrice
            FROM `order` o
            WHERE o.date >= ?
            AND o.status IN (1, 2)
            AND COALESCE(o.annulee, 0) = 0
            AND NOT EXISTS (
                SELECT 1 FROM j_planning jp 
                WHERE jp.order_id = o.id
            )
            ORDER BY o.date ASC
            LIMIT 100
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, fromDate);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", (String) row[0]);
                order.put("orderNumber", (String) row[1]);
                order.put("numeroCommande", (String) row[1]);
                order.put("orderDate", row[2]);
                order.put("date", row[2]);
                order.put("deadline", (String) row[3]);
                order.put("delai", (String) row[3]);
                order.put("cardCount", ((Number) row[4]).intValue());
                order.put("nombreCartes", ((Number) row[4]).intValue());
                order.put("priority", (String) row[5]);
                order.put("priority", (String) row[5]);
                order.put("status", row[6]);
                order.put("totalPrice", row[7]);
                order.put("prixTotal", row[7]);

                orders.add(order);
            }

            log.info(" {} orders loaded for planning (excluding already planned)", orders.size());

            // Debug stats if no orders found
            if (orders.isEmpty()) {
                String countSql = "SELECT COUNT(*) FROM `order` o WHERE o.date >= ? AND o.status IN (1, 2)";
                Query countQuery = entityManager.createNativeQuery(countSql);
                countQuery.setParameter(1, fromDate);
                Number totalOrders = (Number) countQuery.getSingleResult();

                String plannedSql = "SELECT COUNT(DISTINCT jp.order_id) FROM j_planning jp JOIN `order` o ON jp.order_id = o.id WHERE o.date >= ?";
                Query plannedQuery = entityManager.createNativeQuery(plannedSql);
                plannedQuery.setParameter(1, fromDate);
                Number plannedOrders = (Number) plannedQuery.getSingleResult();

                log.info(" Orders stats: Total={}, Planned={}, Remaining={}",
                        totalOrders, plannedOrders, totalOrders.intValue() - plannedOrders.intValue());
            }

            return orders;

        } catch (Exception e) {
            log.error(" Error loading orders for planning: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    /**
     * Get count of already planned orders for diagnostics
     * @return count of orders that already have planning entries
     */
    @Transactional(readOnly = true)
    public long getAlreadyPlannedOrdersCount() {
        try {
            String sql = "SELECT COUNT(DISTINCT jp.order_id) FROM j_planning jp";
            Query query = entityManager.createNativeQuery(sql);
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            log.error("Error counting planned orders: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Check if a specific order is already planned
     * @param orderId the order ID to check
     * @return true if order is already planned
     */
    @Transactional(readOnly = true)
    public boolean isOrderAlreadyPlanned(String orderId) {
        try {
            String sql = "SELECT COUNT(*) FROM j_planning WHERE HEX(order_id) = ?";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, orderId.replace("-", ""));
            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            log.error("Error checking if order is planned: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get all orders as map for compatibility
     * @return list of orders as maps
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllOrdersAsMap() {
        try {
            List<Order> orders = orderRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Order order : orders) {
                Map<String, Object> orderMap = convertOrderToMap(order);
                result.add(orderMap);
            }

            return result;

        } catch (Exception e) {
            log.error("Error retrieving all orders", e);
            return new ArrayList<>();
        }
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate new order business rules
     */
    private void validateNewOrder(Order order) {
        if (order.getCardCount() != null) {
            if (order.getCardCount() < MIN_CARDS_PER_ORDER || order.getCardCount() > MAX_CARDS_PER_ORDER) {
                throw new IllegalArgumentException(
                        String.format("Card count must be between %d and %d", MIN_CARDS_PER_ORDER, MAX_CARDS_PER_ORDER)
                );
            }
        }

        if (order.getOrderNumber() != null && orderRepository.findByOrderNumber(order.getOrderNumber()).isPresent()) {
            throw new IllegalArgumentException("Order number already exists: " + order.getOrderNumber());
        }
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // Define valid transitions
        Set<Order.OrderStatus> validTransitions = new HashSet<>();

        if (currentStatus != null) {
            switch (currentStatus) {
                case PENDING:
                    validTransitions.addAll(Arrays.asList(
                            Order.OrderStatus.SCHEDULED,
                            Order.OrderStatus.IN_PROGRESS,
                            Order.OrderStatus.CANCELLED
                    ));
                    break;
                case SCHEDULED:
                    validTransitions.addAll(Arrays.asList(
                            Order.OrderStatus.IN_PROGRESS,
                            Order.OrderStatus.CANCELLED,
                            Order.OrderStatus.PENDING // Allow going back to pending if needed
                    ));
                    break;
                case IN_PROGRESS:
                    validTransitions.addAll(Arrays.asList(
                            Order.OrderStatus.COMPLETED,
                            Order.OrderStatus.CANCELLED,
                            Order.OrderStatus.SCHEDULED // Allow going back to scheduled if needed
                    ));
                    break;
                case COMPLETED:
                    // Generally, completed orders shouldn't change status
                    // But allow reopening if needed
                    validTransitions.add(Order.OrderStatus.IN_PROGRESS);
                    break;
                case CANCELLED:
                    // Allow reactivating cancelled orders
                    validTransitions.add(Order.OrderStatus.PENDING);
                    break;
            }
        } else {
            // If no current status, allow any status
            validTransitions.addAll(Arrays.asList(Order.OrderStatus.values()));
        }

        if (!validTransitions.contains(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    // ========== SEARCH AND FILTERING ==========

    /**
     * Search orders by various criteria
     * @param searchTerm search term
     * @param status order status filter
     * @param priority priority filter
     * @return list of filtered orders
     */
    @Transactional(readOnly = true)
    public List<Order> searchOrders(String searchTerm, Order.OrderStatus status, Order.OrderPriority priority) {
        // Implementation would depend on specific search requirements
        // For now, return a basic filtered list
        return orderRepository.findAll().stream()
                .filter(order -> searchTerm == null ||
                        order.getOrderNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))
                .filter(order -> status == null || order.getStatus().equals(status))
                .filter(order -> priority == null || order.getPriority().equals(priority))
                .collect(Collectors.toList());
    }

    // ========== STATISTICS METHODS ==========

    /**
     * Get order statistics
     * @return statistics map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Basic counts
            long totalOrders = orderRepository.count();
            long pendingCount = orderRepository.countByStatus(Order.OrderStatus.PENDING);
            long inProgressCount = orderRepository.countByStatus(Order.OrderStatus.IN_PROGRESS);
            long completedCount = orderRepository.countByStatus(Order.OrderStatus.COMPLETED);

            stats.put("totalOrders", totalOrders);
            stats.put("pendingOrders", pendingCount);
            stats.put("inProgressOrders", inProgressCount);
            stats.put("completedOrders", completedCount);

            // Calculate completion rate
            if (totalOrders > 0) {
                double completionRate = (double) completedCount / totalOrders * 100;
                stats.put("completionRatePercent", Math.round(completionRate * 100.0) / 100.0);
            } else {
                stats.put("completionRatePercent", 0.0);
            }

            stats.put("success", true);
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error calculating order statistics", e);
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // ========== UTILITY METHODS ==========


    /**
     * Calculate deadline label based on priority
     */
   private String calculateDeadlineLabel(Order order) {
    if (order.getPriority() == Order.OrderPriority.EXCELSIOR) {
        return "X"; // Most urgent (add this new case)
    } else if (order.getPriority() == Order.OrderPriority.FAST_PLUS) {
        return "F+"; // Urgent
    } else if (order.getPriority() == Order.OrderPriority.FAST) {
        return "F"; // Medium
    } else if (order.getPriority() == Order.OrderPriority.CLASSIC) {
        return "C"; // Normal
    } else {
        return "C"; // Default fallback
    }
}


    /**
     * Get recent orders as maps (for OrderController frontend compatibility)
     * @return list of recent orders as maps
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrdersAsMap() {
        try {
            log.info(" Getting recent orders as maps for frontend");

           // Use the getRecentOrders() method which already works
            return getRecentOrders();

        } catch (Exception e) {
            log.error(" Error in getRecentOrdersAsMap: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ========== COMPATIBILITY METHODS (for migration from CommandeService) ==========

    /**
     * Legacy method for compatibility with existing code
     * @deprecated Use getAllOrdersAsMap() instead
     */
    @Deprecated
    public List<Map<String, Object>> getToutesCommandes() {
        log.warn("Using deprecated method getToutesCommandes(), please use getAllOrdersAsMap()");
        return getAllOrdersAsMap();
    }

    /**
     * Legacy method for compatibility with existing code
     * @deprecated Use getOrdersForPlanning() instead
     */
    @Deprecated
    public List<Map<String, Object>> getCommandesDepuis(int jour, int mois, int annee) {
        log.warn("Using deprecated method getCommandesDepuis(), please use getOrdersForPlanning()");
        return getOrdersForPlanning(jour, mois, annee);
    }

    /**
     * Get recent orders from June 1st, 2025 onwards for frontend display
     * @return list of orders as maps for frontend compatibility
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrders() {
        try {
            log.info(" Frontend: Retrieving orders from June 1st, 2025");

            String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                DATE(o.date) as orderDate,
                o.date as creationDate,
                COALESCE(o.delai, '7 jours') as deadline,
                o.reference as reference,
                o.type as type,
                COALESCE(o.note_minimale, 8.0) as minimumGrade,
                COALESCE(o.nb_descellements, 0) as unsealing,
                o.status,
                COALESCE(
                    (SELECT COUNT(*) FROM card_certification_order cco2 
                     WHERE cco2.order_id = o.id), 
                    CASE 
                        WHEN o.type >= 10 THEN FLOOR(5 + RAND() * 25)
                        WHEN o.type >= 5 THEN FLOOR(3 + RAND() * 15) 
                        ELSE FLOOR(1 + RAND() * 10)
                    END
                ) as cardCount
            FROM `order` o
            WHERE o.date >= '2025-06-01'
            AND o.status IN (1, 2)
            AND COALESCE(o.annulee, 0) = 0
            ORDER BY o.date DESC
            LIMIT 50
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            log.info(" Orders found: {}", resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> order = new HashMap<>();

                // Basic data
                order.put("id", (String) row[0]);
                order.put("orderNumber", (String) row[1]);
                order.put("orderDate", row[2]);
                order.put("creationDate", row[3]);
                order.put("deadline", (String) row[4]);
                order.put("reference", row[5]);
                order.put("type", row[6]);
                order.put("minimumGrade", row[7]);
                order.put("unsealing", row[8]);
                order.put("status", row[9]);
                order.put("cardCount", ((Number) row[10]).intValue());

                // Additional calculations
                int cardCount = ((Number) row[10]).intValue();
                order.put("estimatedTimeMinutes", cardCount * DEFAULT_PROCESSING_TIME_PER_CARD);
                order.put("estimatedTimeHours", String.format("%.1fh",
                        (cardCount * DEFAULT_PROCESSING_TIME_PER_CARD) / 60.0));

               // Type-based priority
                String priority = "FAST";
                if (row[6] != null) {
                    int type = ((Number) row[6]).intValue();
                    if (type >= 15) priority = "FAST+";
                    else if (type >= 10) priority = "FAST";
                    else priority = "CLASSIC";
                }
                order.put("priority", priority);

                // Status text mapping
                String statusText = "Unknown";
                if (row[9] != null) {
                    int status = ((Number) row[9]).intValue();
                    switch (status) {
                        case 1 -> statusText = "PENDING";
                        case 2 -> statusText = "IN_PROGRESS";
                        case 3 -> statusText = "COMPLETED";
                        default -> statusText = "UNKNOWN";
                    }
                }
                order.put("statusText", statusText);

                // Default values
                order.put("totalPrice", 150.0 + (cardCount * 5.0));
                order.put("qualityIndicator", cardCount > 20 ? "" : cardCount > 10 ? "" : "");

                orders.add(order);
            }

            log.info(" {} orders successfully retrieved for frontend", orders.size());
            return orders;

        } catch (Exception e) {
            log.error(" Error retrieving recent orders: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }



}