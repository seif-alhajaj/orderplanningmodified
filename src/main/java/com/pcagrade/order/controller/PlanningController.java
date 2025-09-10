package com.pcagrade.order.controller;

import com.pcagrade.order.service.EmployeeService;
import com.pcagrade.order.service.GreedyPlanningService;
import com.pcagrade.order.service.PlanningService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planning")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PlanningController {
    private static final Logger log = LoggerFactory.getLogger(PlanningController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PlanningService planningService; //Use existing service
    @Autowired
    private GreedyPlanningService greedyPlanningService; // Alternative

    /**
     *  ENDPOINT PRINCIPAL - Utilise PlanningService existant
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePlanning(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info(" FINAL WORKING PLANNING GENERATION");

            // ========== PARAMETERS ==========
            String startDate = (String) request.getOrDefault("startDate", "2025-07-01");
            Integer timePerCard = request.containsKey("timePerCard") ?
                    Integer.valueOf(request.get("timePerCard").toString()) : 3;
            Boolean cleanFirst = (Boolean) request.getOrDefault("cleanFirst", false);

            log.info(" Config: startDate={}, timePerCard={}, cleanFirst={}",
                    startDate, timePerCard, cleanFirst);

            // ========== AGGRESSIVE CLEAN ==========
            int deletedCount = 0;
            if (cleanFirst) {
                try {
                    // Try multiple approaches to ensure clean
                    String[] deleteQueries = {
                            "DELETE FROM j_planning WHERE 1=1",
                            "TRUNCATE TABLE j_planning"
                    };

                    for (String deleteQuery : deleteQueries) {
                        try {
                            Query deleteQ = entityManager.createNativeQuery(deleteQuery);
                            deletedCount += deleteQ.executeUpdate();
                            log.info(" Delete query '{}' removed {} rows", deleteQuery, deletedCount);
                            break; // Stop after first successful delete
                        } catch (Exception e) {
                            log.warn("Delete query '{}' failed: {}", deleteQuery, e.getMessage());
                        }
                    }

                    // Verify clean worked
                    Query countQ = entityManager.createNativeQuery("SELECT COUNT(*) FROM j_planning");
                    Number remaining = (Number) countQ.getSingleResult();
                    log.info(" After clean: {} plannings remaining", remaining.intValue());

                } catch (Exception cleanError) {
                    log.error(" All clean attempts failed: {}", cleanError.getMessage());
                }
            }

            // ========== GET DATA ==========
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                result.put("success", false);
                result.put("message", "No active employees found");
                return ResponseEntity.ok(result);
            }

            // ========== SIMPLE ORDER QUERY WITHOUT NOT EXISTS ==========
            String orderQuery = """
                SELECT 
                    HEX(o.id) as orderId,
                    o.num_commande as orderNumber,
                    o.date as orderDate
                FROM `order` o
                WHERE o.date >= ?
                ORDER BY o.date ASC
                LIMIT 10
            """;

            Query query = entityManager.createNativeQuery(orderQuery);
            query.setParameter(1, startDate);
            @SuppressWarnings("unchecked")
            List<Object[]> orderResults = query.getResultList();

            log.info(" Found {} orders total from date {}", orderResults.size(), startDate);

            if (orderResults.isEmpty()) {
                result.put("success", false);
                result.put("message", "No orders found from date: " + startDate);
                result.put("processedOrders", 0);
                return ResponseEntity.ok(result);
            }

            // ========== FORCE SAVE ALL ORDERS ==========
            List<Map<String, Object>> savedPlannings = new ArrayList<>();
            int successCount = 0;
            List<String> saveErrors = new ArrayList<>();

            for (int i = 0; i < orderResults.size(); i++) {
                try {
                    Object[] orderData = orderResults.get(i);
                    String orderId = (String) orderData[0];
                    String orderNumber = (String) orderData[1];

                    Map<String, Object> employee = employees.get(i % employees.size());
                    String employeeId = (String) employee.get("id");
                    String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                    int cardCount = 20;
                    int durationMinutes = cardCount * timePerCard;
                    LocalDateTime startTime = LocalDate.parse(startDate).atTime(9, 0).plusHours(i);

                    String planningId = UUID.randomUUID().toString().replace("-", "");

                    // ========== MOST BASIC INSERT POSSIBLE ==========
                    String insertQuery = """
                        INSERT INTO j_planning (id, order_id, employee_id) 
                        VALUES (UNHEX(?), UNHEX(?), UNHEX(?))
                    """;

                    Query insertQ = entityManager.createNativeQuery(insertQuery);
                    insertQ.setParameter(1, planningId);
                    insertQ.setParameter(2, orderId);
                    insertQ.setParameter(3, employeeId);

                    int rowsInserted = insertQ.executeUpdate();

                    if (rowsInserted > 0) {
                        successCount++;

                        Map<String, Object> planning = new HashMap<>();
                        planning.put("planningId", planningId);
                        planning.put("orderId", orderId);
                        planning.put("orderNumber", orderNumber);
                        planning.put("employeeId", employeeId);
                        planning.put("employeeName", employeeName);
                        planning.put("cardCount", cardCount);
                        planning.put("durationMinutes", durationMinutes);
                        planning.put("status", "BASIC_SAVED");

                        savedPlannings.add(planning);
                        log.info("BASIC SAVE #{}: Order {} -> Employee {}",
                                successCount, orderNumber, employeeName);
                    }

                } catch (Exception orderError) {
                    String error = String.format("Order %s failed: %s",
                            orderResults.get(i)[1], orderError.getMessage());
                    saveErrors.add(error);
                    log.error(" {}", error);
                }
            }

            // ========== TRY ENHANCED SAVES FOR SAVED ITEMS ==========
            for (Map<String, Object> savedPlanning : savedPlannings) {
                try {
                    String planningId = (String) savedPlanning.get("planningId");
                    int cardCount = (Integer) savedPlanning.get("cardCount");
                    int durationMinutes = (Integer) savedPlanning.get("durationMinutes");
                    LocalDateTime startTime = LocalDate.parse(startDate).atTime(9, 0);

                    String updateQuery = """
                        UPDATE j_planning 
                        SET planning_date = ?, start_time = ?, 
                            estimated_duration_minutes = ?, card_count = ?, 
                            status = 'PLANNED', created_at = NOW(), updated_at = NOW()
                        WHERE id = UNHEX(?)
                    """;

                    Query updateQ = entityManager.createNativeQuery(updateQuery);
                    updateQ.setParameter(1, startTime.toLocalDate());
                    updateQ.setParameter(2, startTime.toLocalTime());
                    updateQ.setParameter(3, durationMinutes);
                    updateQ.setParameter(4, cardCount);
                    updateQ.setParameter(5, planningId);

                    int rowsUpdated = updateQ.executeUpdate();
                    if (rowsUpdated > 0) {
                        savedPlanning.put("status", "FULLY_CONFIGURED");
                        log.info(" Enhanced save for planning {}", planningId);
                    }

                } catch (Exception updateError) {
                    log.warn(" Enhancement failed for planning: {}", updateError.getMessage());
                }
            }

            // ========== FINAL VERIFICATION ==========
            Query finalCountQ = entityManager.createNativeQuery("SELECT COUNT(*) FROM j_planning");
            Number totalPlannings = (Number) finalCountQ.getSingleResult();

            // ========== RESULT ==========
            boolean hasSuccess = successCount > 0;
            result.put("success", hasSuccess);
            result.put("message", hasSuccess ?
                    String.format(" SUCCESS: %d plannings saved to database!", successCount) :
                    " No plannings could be saved - see errors");
            result.put("processedOrders", successCount);
            result.put("totalOrdersAnalyzed", orderResults.size());
            result.put("activeEmployees", employees.size());
            result.put("planningsCreated", savedPlannings);
            result.put("totalPlanningsInDB", totalPlannings.intValue());
            result.put("deletedBefore", deletedCount);
            result.put("method", "BASIC_INSERT_THEN_ENHANCE");
            result.put("timePerCard", timePerCard);
            result.put("startDate", startDate);
            result.put("timestamp", System.currentTimeMillis());

            if (!saveErrors.isEmpty()) {
                result.put("errors", saveErrors);
            }

            log.info(" FINAL RESULT: {} plannings saved, {} total in DB",
                    successCount, totalPlannings.intValue());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Final planning generation failed: {}", e.getMessage(), e);

            result.put("success", false);
            result.put("message", "Final generation failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("detailedError", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        }
    }

    /**
     *  DIAGNOSIS - Analyze available services
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugPlanning(
            @RequestParam(defaultValue = "2025-07-01") String startDate) {

        Map<String, Object> debug = new HashMap<>();

        try {
            log.info(" DIAGNOSTIC - Services and data analysis");

            // ========== CHECK EMPLOYEES ==========
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            debug.put("activeEmployees", employees.size());
            debug.put("employees", employees.stream().limit(3).toList());

            // ========== CHECK ORDERS ==========
            String orderQuery = """
                SELECT 
                    HEX(o.id) as orderId,
                    o.num_commande as orderNumber,
                    o.date as orderDate,
                    COALESCE(o.priority, 'FAST') as priority,
                    COALESCE(o.status, 1) as status
                FROM `order` o
                WHERE o.date >= ?
                ORDER BY o.date ASC
                LIMIT 10
            """;

            Query query = entityManager.createNativeQuery(orderQuery);
            query.setParameter(1, startDate);
            @SuppressWarnings("unchecked")
            List<Object[]> orderResults = query.getResultList();

            debug.put("ordersFromDate", orderResults.size());
            debug.put("sampleOrders", orderResults.stream().limit(3).map(row -> {
                Map<String, Object> order = new HashMap<>();
                order.put("orderId", row[0]);
                order.put("orderNumber", row[1]);
                order.put("orderDate", row[2]);
                order.put("priority", row[3]);
                order.put("status", row[4]);
                return order;
            }).toList());

            // ========== CHECK EXISTING PLANNINGS ==========
            Query planningCountQ = entityManager.createNativeQuery("SELECT COUNT(*) FROM j_planning");
            Number planningCount = (Number) planningCountQ.getSingleResult();
            debug.put("existingPlannings", planningCount.intValue());

            // ========== SERVICE AVAILABILITY ==========
            Map<String, Object> services = new HashMap<>();
            services.put("planningService", planningService != null ? "AVAILABLE" : "NOT_INJECTED");
            services.put("greedyPlanningService", greedyPlanningService != null ? "AVAILABLE" : "NOT_INJECTED");
            services.put("employeeService", employeeService != null ? "AVAILABLE" : "NOT_INJECTED");
            debug.put("services", services);

            // ========== SUMMARY ==========
            debug.put("success", true);
            debug.put("analysis", Map.of(
                    "startDate", startDate,
                    "ordersAvailable", orderResults.size(),
                    "employeesAvailable", employees.size(),
                    "existingPlannings", planningCount.intValue(),
                    "servicesInjected", services.values().stream().allMatch(v -> "AVAILABLE".equals(v))
            ));

            List<String> recommendations = new ArrayList<>();
            if (orderResults.isEmpty()) {
                recommendations.add(" No orders found from date " + startDate);
            } else if (employees.isEmpty()) {
                recommendations.add(" No employees available");
            } else {
                recommendations.add(" " + orderResults.size() + " orders and " + employees.size() + " employees available");
            }
            debug.put("recommendations", recommendations);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            log.error(" Debug failed: {}", e.getMessage(), e);
            debug.put("success", false);
            debug.put("error", e.getMessage());
            return ResponseEntity.ok(debug);
        }
    }

    /**
     *  GET ALL PLANNINGS - retrieves all plannings
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPlannings() {
        try {
            log.info(" Fetching all plannings from j_planning table");

            String sql = """
                SELECT 
                    HEX(p.id) as id,
                    HEX(p.order_id) as orderId,
                    HEX(p.employee_id) as employeeId,
                    p.planning_date,
                    p.start_time,
                    p.estimated_duration_minutes,
                    p.priority,
                    p.status,
                    p.completed,
                    p.card_count,
                    p.progress_percentage,
                    p.created_at,
                    p.updated_at,
                    o.num_commande as orderNumber,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as employeeName
                FROM j_planning p
                LEFT JOIN `order` o ON p.order_id = o.id  
                LEFT JOIN j_employee e ON p.employee_id = e.id
                ORDER BY p.planning_date ASC, p.start_time ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> plannings = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> planning = new HashMap<>();
                planning.put("id", row[0]);
                planning.put("orderId", row[1]);
                planning.put("employeeId", row[2]);
                planning.put("planningDate", row[3]);
                planning.put("startTime", row[4]);
                planning.put("estimatedDurationMinutes", row[5]);
                planning.put("priority", row[6]);
                planning.put("status", row[7]);
                planning.put("completed", row[8]);
                planning.put("cardCount", row[9]);
                planning.put("progressPercentage", row[10]);
                planning.put("createdAt", row[11]);
                planning.put("updatedAt", row[12]);
                planning.put("orderNumber", row[13]);
                planning.put("employeeName", row[14]);

                // Formatted duration
                Integer duration = (Integer) row[5];
                if (duration != null) {
                    planning.put("formattedDuration", formatDuration(duration));
                    planning.put("estimatedHours", Math.round(duration / 60.0 * 100.0) / 100.0);
                }

                plannings.add(planning);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("plannings", plannings);
            response.put("total", plannings.size());

            log.info(" Retrieved {} plannings successfully", plannings.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error fetching plannings", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     *  GET EMPLOYEE SCHEDULES - Schedules for a specific employee
* GET EMPLOYEE SCHEDULES - Compatible with unified algorithm
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeePlannings(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        try {
            log.info("Loading plannings for employee: {}, date: {}", employeeId, date);

            String cleanEmployeeId = employeeId.replace("-", "");

            String sql = """
            SELECT 
                HEX(p.id) as planningId,
                HEX(p.order_id) as orderId,
                o.num_commande as orderNumber,
                DATE(p.planning_date) as planningDate,
                TIME(p.start_time) as startTime,
                p.estimated_duration_minutes as durationMinutes,
                p.priority,
                p.status,
                p.card_count as cardCount,
                p.notes
            FROM j_planning p
            LEFT JOIN `order` o ON p.order_id = o.id  
            WHERE HEX(p.employee_id) = ?
            ORDER BY p.planning_date DESC, p.start_time ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, cleanEmployeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            log.info("Found {} total plannings for employee {}", results.size(), employeeId);
            if (!results.isEmpty()) {
                log.info("Planning dates for this employee: {}",
                        results.stream().map(r -> r[3]).distinct().collect(Collectors.toList()));
            }

            // Apply date filter only if specified
            if (date != null && !date.isEmpty()) {
                results = results.stream()
                        .filter(row -> date.equals(row[3].toString()))
                        .collect(Collectors.toList());
                log.info("After date filter ({}): {} plannings", date, results.size());
            }

            // If no results with date filter, show all plannings
            if (results.isEmpty() && date != null) {
                log.warn("No plannings found for date {}, showing all plannings for employee", date);
                query = entityManager.createNativeQuery(sql);
                query.setParameter(1, cleanEmployeeId.toUpperCase());
                results = query.getResultList();
            }

            List<Map<String, Object>> orders = new ArrayList<>();
            int totalCards = 0;
            int totalDuration = 0;

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();

                order.put("planningId", row[0] != null ? row[0].toString() : "");
                order.put("orderId", row[1] != null ? row[1].toString() : "");
                order.put("orderNumber", row[2] != null ? row[2].toString() : "Unknown Order");
                order.put("planningDate", row[3] != null ? row[3].toString() : "");
                order.put("startTime", row[4] != null ? row[4].toString() : "09:00");

                // Safe conversion for numeric fields
                Integer duration = 60; // default
                if (row[5] != null) {
                    if (row[5] instanceof Number) {
                        duration = ((Number) row[5]).intValue();
                    } else {
                        try {
                            duration = Integer.parseInt(row[5].toString());
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse duration: {}", row[5]);
                        }
                    }
                }
                order.put("durationMinutes", duration);
                order.put("estimatedDuration", duration);

                order.put("priority", row[6] != null ? row[6].toString() : "FAST");
                order.put("status", row[7] != null ? row[7].toString() : "SCHEDULED");

                // Safe conversion for card count
                Integer cardCount = 10; // default
                if (row[8] != null) {
                    if (row[8] instanceof Number) {
                        cardCount = ((Number) row[8]).intValue();
                    } else {
                        try {
                            cardCount = Integer.parseInt(row[8].toString());
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse card count: {}", row[8]);
                        }
                    }
                }
                order.put("cardCount", cardCount);

                order.put("notes", row[9] != null ? row[9].toString() : "");

                // Add compatibility fields
                order.put("id", row[1] != null ? row[1].toString() : "");
                order.put("showCards", false);
                order.put("loadingCards", false);
                order.put("cards", new ArrayList<>());

                orders.add(order);

                totalCards += cardCount;
                totalDuration += duration;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("summary", Map.of(
                    "totalOrders", orders.size(),
                    "totalCards", totalCards,
                    "totalDuration", totalDuration,
                    "estimatedHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0
            ));

            log.info("Returning {} orders for employee {}", orders.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error loading employee plannings: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orders", new ArrayList<>());

            return ResponseEntity.ok(errorResponse);
        }
    }
    /**
     *  GET EMPLOYEES WITH PLANNING DATA - Employees with their statistics
     */
    @GetMapping("/employees-stats")
    public ResponseEntity<Map<String, Object>> getEmployeesWithPlanningStats(
            @RequestParam(required = false) String date) {

        try {
            log.info(" Fetching employees with planning stats for date: {}", date);

            String dateFilter = date != null ? " AND p.planning_date = '" + date + "'" : "";

            String sql = """
                SELECT 
                    HEX(e.id) as employeeId,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name,
                    e.last_name,
                    e.email,
                    e.active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    COALESCE(SUM(p.estimated_duration_minutes), 0) as totalMinutes,
                    COUNT(p.id) as taskCount,
                    COALESCE(SUM(p.card_count), 0) as cardCount,
                    ROUND(COALESCE(SUM(p.estimated_duration_minutes), 0) / (COALESCE(e.work_hours_per_day, 8) * 60.0), 2) as workloadRatio
                FROM j_employee e
                LEFT JOIN j_planning p ON e.id = p.employee_id""" + dateFilter + """
                GROUP BY e.id, e.first_name, e.last_name, e.email, e.active, e.work_hours_per_day
                ORDER BY workloadRatio DESC, name ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                 employee.put("id", row[0]);
                 employee.put("name", row[1]);
                 employee.put("firstName", row[2]);
                 employee.put("lastName", row[3]);
                 employee.put("email", row[4]);
                 employee.put("role", row[5]);         // ← ADDED ROLE FIELD
                 employee.put("active", row[6]);       // active moved to index 6
                 employee.put("workHoursPerDay", row[7]);  // workHoursPerDay moved to index 7
                 employee.put("totalMinutes", row[8]);     // totalMinutes moved to index 8
                 employee.put("maxMinutes", ((Number) row[7]).intValue() * 60);
                 employee.put("taskCount", row[9]);        // taskCount moved to index 9
                 employee.put("cardCount", row[10]);       // cardCount moved to index 10
                 employee.put("workloadRatio", row[11]);   // workloadRatio moved to index 11

                // Status based on workload
                Double workloadRatio = (Double) row[10];
                String status;
                if (workloadRatio >= 1.0) {
                    status = "overloaded";
                } else if (workloadRatio >= 0.8) {
                    status = "busy";
                } else {
                    status = "available";
                }
                employee.put("status", status);
                employee.put("available", workloadRatio < 0.8);

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());
            response.put("date", date != null ? date : "all");

            log.info(" Retrieved {} employees with planning stats", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error fetching employees with planning stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     *  DELETE ALL PLANNINGS - cleans the table j_planning
     */
    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupPlannings() {
        try {
            log.info(" Cleaning up j_planning table");

            String countSql = "SELECT COUNT(*) FROM j_planning";
            Query countQuery = entityManager.createNativeQuery(countSql);
            Number beforeCount = (Number) countQuery.getSingleResult();

            String deleteSql = "DELETE FROM j_planning";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deletedRows = deleteQuery.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Planning table cleaned successfully");
            response.put("rowsDeleted", deletedRows);
            response.put("beforeCount", beforeCount.intValue());

            log.info(" Deleted {} planning records", deletedRows);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error cleaning planning table", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     *  GET PLANNING STATS - Statistiques globales
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlanningStats() {
        try {
            log.info(" Fetching planning statistics");

            String sql = """
                SELECT 
                    COUNT(*) as totalPlannings,
                    COUNT(DISTINCT employee_id) as employeesUsed,
                    COUNT(DISTINCT order_id) as ordersPlanned,
                    SUM(card_count) as totalCards,
                    SUM(estimated_duration_minutes) as totalMinutes,
                    AVG(estimated_duration_minutes) as avgDuration,
                    COUNT(CASE WHEN status = 'SCHEDULED' THEN 1 END) as scheduled,
                    COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as inProgress,
                    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
                    COUNT(CASE WHEN priority = 'FAST_PLUS' THEN 1 END) as highPriority,
                    MIN(planning_date) as earliestDate,
                    MAX(planning_date) as latestDate
                FROM j_planning
                """;

            Query query = entityManager.createNativeQuery(sql);
            Object[] result = (Object[]) query.getSingleResult();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPlannings", result[0]);
            stats.put("employeesUsed", result[1]);
            stats.put("ordersPlanned", result[2]);
            stats.put("totalCards", result[3]);
            stats.put("totalMinutes", result[4]);
            stats.put("totalHours", result[4] != null ? Math.round(((Number) result[4]).doubleValue() / 60.0 * 100.0) / 100.0 : 0);
            stats.put("avgDuration", result[5] != null ? Math.round(((Number) result[5]).doubleValue() * 100.0) / 100.0 : 0);
            stats.put("scheduled", result[6]);
            stats.put("inProgress", result[7]);
            stats.put("completed", result[8]);
            stats.put("highPriority", result[9]);
            stats.put("earliestDate", result[10]);
            stats.put("latestDate", result[11]);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            log.info(" Planning stats retrieved successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error fetching planning stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ========== UTILITY METHODS ==========

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "min";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return remainingMinutes > 0 ? hours + "h" + remainingMinutes + "min" : hours + "h";
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Pokemon Card Planning");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     *  GREEDY PLANNING ENDPOINT - Uses GreedyPlanningService
     */
    @PostMapping("/greedy")
    public ResponseEntity<Map<String, Object>> executeGreedyPlanning(@RequestBody Map<String, Object> request) {
        try {
            log.info(" GREEDY PLANNING EXECUTION");

            // ========== PARAMETERS ==========
            Integer day = (Integer) request.getOrDefault("day", 1);
            Integer month = (Integer) request.getOrDefault("month", 6);
            Integer year = (Integer) request.getOrDefault("year", 2025);
            Integer timePerCard = (Integer) request.getOrDefault("timePerCard", 3);

            log.info(" Greedy Config: date={}/{}/{}, timePerCard={}", day, month, year, timePerCard);

            // ========== CALL GREEDY SERVICE ==========
            Map<String, Object> result = greedyPlanningService.executeGreedyPlanning(day, month, year);

            log.info(" Greedy planning result: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Greedy planning failed: {}", e.getMessage(), e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Greedy planning failed: " + e.getMessage());
            errorResult.put("error", e.getClass().getSimpleName());
            errorResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(errorResult);
        }
    }
    /**
     *  ULTRA SIMPLE GREEDY PLANNING - Uses existing services only
     */
    /**
     *  ULTRA SIMPLE GREEDY PLANNING - Uses existing services only
     */
    @PostMapping("/greedy-simple")
    @Transactional
    public ResponseEntity<Map<String, Object>> executeUltraSimpleGreedy(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info(" ULTRA SIMPLE GREEDY PLANNING");

            // ========== PARAMETERS ==========
            String startDate = (String) request.getOrDefault("startDate", "2025-06-01");
            Integer timePerCard = (Integer) request.getOrDefault("timePerCard", 3);

            log.info(" Simple Greedy Config: startDate={}, timePerCard={}", startDate, timePerCard);

            // ========== USE EXISTING WORKING QUERIES FROM DEBUG ENDPOINT ==========

            // 1. Get employees (this works from debug endpoint)
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                result.put("success", false);
                result.put("message", "No active employees found");
                return ResponseEntity.ok(result);
            }
            log.info(" Found {} active employees", employees.size());

            // 2. Get orders using the real database structure with card count
            String orderQuery = """
            SELECT 
                HEX(o.id) as orderId,
                o.num_commande as orderNumber,
                o.date as orderDate,
                COALESCE(o.priority, 'FAST') as priority,
                COALESCE(o.status, 1) as status,
                (SELECT COUNT(*) 
                 FROM card_certification_order cco 
                 WHERE cco.order_id = o.id) as cardCount
            FROM `order` o
            WHERE o.date >= ?
            ORDER BY o.date ASC
            LIMIT 50
            """;

            Query orderQ = entityManager.createNativeQuery(orderQuery);
            orderQ.setParameter(1, LocalDate.parse(startDate));
            List<Object[]> orderResults = orderQ.getResultList();

            if (orderResults.isEmpty()) {
                result.put("success", true);
                result.put("message", "No orders found from date " + startDate);
                result.put("plannings", new ArrayList<>());
                return ResponseEntity.ok(result);
            }
            log.info(" Found {} orders to plan", orderResults.size());

            // 3. Simple planning assignment - one order per employee, round-robin
            List<Map<String, Object>> createdPlannings = new ArrayList<>();
            int successCount = 0;

            for (int i = 0; i < orderResults.size(); i++) {
                try {
                    Object[] orderRow = orderResults.get(i);
                    String orderId = (String) orderRow[0];
                    String orderNumber = (String) orderRow[1];
                    Number cardCountNumber = (Number) orderRow[5]; // cardCount is now at index 5
                    int cardCount = cardCountNumber != null ? cardCountNumber.intValue() : 10; // Use real count or default

                    // Round-robin employee assignment
                    Map<String, Object> employee = employees.get(i % employees.size());
                    String employeeId = (String) employee.get("id");
                    String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                    // Calculate duration based on real card count
                    int durationMinutes = Math.max(15, cardCount * timePerCard); // Minimum 15 minutes
                    String planningId = UUID.randomUUID().toString().replace("-", "");

                    // INSERT using the exact format from GreedyPlanningService that works
                    String insertSql = """
                    INSERT INTO j_planning 
                    (id, order_id, employee_id, planning_date, start_time, end_time, 
                     estimated_duration_minutes, estimated_end_time, priority, status, 
                     completed, card_count, notes, created_at, updated_at)
                    VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """;

                    // Calculate times like in GreedyPlanningService
                    LocalDateTime startDateTime = LocalDate.parse(startDate).atTime(9, 0).plusMinutes(i * 30);
                    LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

                    Query insertQ = entityManager.createNativeQuery(insertSql);
                    insertQ.setParameter(1, planningId);
                    insertQ.setParameter(2, orderId.replace("-", ""));
                    insertQ.setParameter(3, employeeId.replace("-", ""));
                    insertQ.setParameter(4, LocalDate.parse(startDate)); // planning_date
                    insertQ.setParameter(5, startDateTime); // start_time (DATETIME)
                    insertQ.setParameter(6, endDateTime); // end_time
                    insertQ.setParameter(7, durationMinutes); // estimated_duration_minutes
                    insertQ.setParameter(8, endDateTime); // estimated_end_time
                    insertQ.setParameter(9, "CLASSIC"); // priority (ENUM)
                    insertQ.setParameter(10, "SCHEDULED"); // status (ENUM) - correct value!
                    insertQ.setParameter(11, 0); // completed (BOOLEAN as INT)
                    insertQ.setParameter(12, cardCount); // card_count
                    insertQ.setParameter(13, String.format("Auto-generated planning for %d cards", cardCount)); // notes

                    int rowsInserted = insertQ.executeUpdate();

                    if (rowsInserted > 0) {
                        successCount++;

                        Map<String, Object> planning = new HashMap<>();
                        planning.put("planningId", planningId);
                        planning.put("orderId", orderId);
                        planning.put("orderNumber", orderNumber);
                        planning.put("employeeId", employeeId);
                        planning.put("employeeName", employeeName);
                        planning.put("cardCount", cardCount);
                        planning.put("durationMinutes", durationMinutes);
                        planning.put("status", "PLANNED");

                        createdPlannings.add(planning);
                        log.info(" Simple Planning #{}: Order {} -> Employee {}", successCount, orderNumber, employeeName);
                    }

                } catch (Exception orderError) {
                    log.warn(" Failed to plan order {}: {}", i, orderError.getMessage());
                    // Continue with next order
                }
            }

            // ========== FORCE COMMIT ==========
            entityManager.flush();

            // ========== RESULT ==========
            result.put("success", true);
            result.put("message", String.format(" ULTRA SIMPLE SUCCESS: %d plannings created", successCount));
            result.put("plannings", createdPlannings);
            result.put("planningsCount", successCount);
            result.put("ordersAnalyzed", orderResults.size());
            result.put("employeesUsed", employees.size());
            result.put("algorithm", "ULTRA_SIMPLE");

            log.info(" ULTRA SIMPLE SUCCESS: {} plannings created from {} orders", successCount, orderResults.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Ultra simple planning failed: {}", e.getMessage(), e);

            result.put("success", false);
            result.put("message", "Ultra simple planning failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        }
    }

    /**
     *  GENERATE UNIFIED PLANNING - Uses same algorithm as Global Planning
     */
    /**
     *  GENERATE UNIFIED PLANNING - Uses same algorithm as Global Planning
     */
    @PostMapping("/generate-unified")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateUnifiedPlanning(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info(" UNIFIED PLANNING GENERATION - Using Global Planning Algorithm");

            String startDate = (String) request.getOrDefault("startDate", "2025-06-01"); // t1
            String planningDate = (String) request.getOrDefault("planningDate", LocalDate.now().toString()); // t2
            Integer timePerCard = (Integer) request.getOrDefault("timePerCard", 3);
            Boolean cleanFirst = (Boolean) request.getOrDefault("cleanFirst", true);

            log.info(" Config: startDate={} (t1), planningDate={} (t2), timePerCard={}, cleanFirst={}",
                    startDate, planningDate, timePerCard, cleanFirst);

            // Clean existing plannings for the planning date (t2)
            if (cleanFirst) {
                String deleteSql = "DELETE FROM j_planning WHERE DATE(planning_date) = ?";
                Query deleteQ = entityManager.createNativeQuery(deleteSql);
                deleteQ.setParameter(1, LocalDate.parse(planningDate));
                int deleted = deleteQ.executeUpdate();
                log.info(" Cleaned {} existing plannings for planning date {}", deleted, planningDate);
            }

            // Get active employees
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                result.put("success", false);
                result.put("message", "No active employees found");
                return ResponseEntity.ok(result);
            }

            // Get orders from t1 onwards
            String orderQuery = """
            SELECT 
                HEX(o.id) as orderId,
                o.num_commande as orderNumber,
                o.date as orderDate,
                COALESCE(o.priority, 'FAST') as priority,
                COALESCE(o.status, 1) as status,
                (SELECT COUNT(*) 
                 FROM card_certification_order cco 
                 WHERE cco.order_id = o.id) as cardCount
            FROM `order` o
            WHERE o.date >= ?
            ORDER BY o.date ASC, o.priority DESC
            LIMIT 50
            """;

            Query orderQ = entityManager.createNativeQuery(orderQuery);
            orderQ.setParameter(1, LocalDate.parse(startDate)); // t1
            List<Object[]> orderResults = orderQ.getResultList();

            if (orderResults.isEmpty()) {
                result.put("success", true);
                result.put("message", "No orders found from date " + startDate);
                result.put("employeeAssignments", new ArrayList<>());
                return ResponseEntity.ok(result);
            }

            log.info(" Found {} orders (from {}) and {} employees", orderResults.size(), startDate, employees.size());

            // SAME ALGORITHM AS GLOBAL PLANNING - Round-robin assignment
            List<Map<String, Object>> employeeAssignments = new ArrayList<>();
            Map<String, List<Map<String, Object>>> employeeOrdersMap = new HashMap<>();

            // Initialize employee assignments
            for (Map<String, Object> employee : employees) {
                String employeeId = (String) employee.get("id");
                String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                employeeOrdersMap.put(employeeId, new ArrayList<>());

                Map<String, Object> assignment = new HashMap<>();
                assignment.put("employeeId", employeeId);
                assignment.put("employeeName", employeeName);
                assignment.put("firstName", employee.get("firstName"));
                assignment.put("lastName", employee.get("lastName"));
                assignment.put("email", employee.get("email"));
                assignment.put("workHoursPerDay", employee.get("workHoursPerDay"));
                assignment.put("orders", new ArrayList<>());
                assignment.put("totalCards", 0);
                assignment.put("totalDuration", 0);
                assignment.put("orderCount", 0);

                employeeAssignments.add(assignment);
            }

            int successCount = 0;

            // Round-robin assignment (SAME AS GLOBAL PLANNING)
            for (int i = 0; i < orderResults.size(); i++) {
                try {
                    Object[] orderRow = orderResults.get(i);
                    String orderId = (String) orderRow[0];
                    String orderNumber = (String) orderRow[1];
                    Number cardCountNumber = (Number) orderRow[5];
                    int cardCount = cardCountNumber != null ? cardCountNumber.intValue() : 10;

                    // Round-robin employee selection
                    Map<String, Object> employee = employees.get(i % employees.size());
                    String employeeId = (String) employee.get("id");
                    String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                    int durationMinutes = Math.max(15, cardCount * timePerCard);
                    String planningId = UUID.randomUUID().toString().replace("-", "");

                    // Insert planning for t2 (planning date), not t1
                    String insertSql = """
                    INSERT INTO j_planning 
                    (id, order_id, employee_id, planning_date, start_time, end_time, 
                     estimated_duration_minutes, estimated_end_time, priority, status, 
                     completed, card_count, notes, created_at, updated_at)
                    VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """;

                    // Plan for t2 (planning date), not t1 (start date)
                    LocalDateTime startDateTime = LocalDate.parse(planningDate).atTime(9, 0).plusMinutes(i * 30);
                    LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

                    Query insertQ = entityManager.createNativeQuery(insertSql);
                    insertQ.setParameter(1, planningId);
                    insertQ.setParameter(2, orderId.replace("-", ""));
                    insertQ.setParameter(3, employeeId.replace("-", ""));
                    insertQ.setParameter(4, LocalDate.parse(planningDate)); // t2 - planning date
                    insertQ.setParameter(5, startDateTime);
                    insertQ.setParameter(6, endDateTime);
                    insertQ.setParameter(7, durationMinutes);
                    insertQ.setParameter(8, endDateTime);
                    insertQ.setParameter(9, "FAST");
                    insertQ.setParameter(10, "SCHEDULED");
                    insertQ.setParameter(11, 0);
                    insertQ.setParameter(12, cardCount);
                    insertQ.setParameter(13, String.format("Order from %s planned for %s - %d cards",
                            orderRow[2], planningDate, cardCount));

                    int rowsInserted = insertQ.executeUpdate();

                    if (rowsInserted > 0) {
                        successCount++;

                        // Add order to employee assignment
                        Map<String, Object> orderInfo = new HashMap<>();
                        orderInfo.put("orderId", orderId);
                        orderInfo.put("orderNumber", orderNumber);
                        orderInfo.put("cardCount", cardCount);
                        orderInfo.put("durationMinutes", durationMinutes);
                        orderInfo.put("startTime", startDateTime.toLocalTime().toString());
                        orderInfo.put("endTime", endDateTime.toLocalTime().toString());
                        orderInfo.put("status", "SCHEDULED");
                        orderInfo.put("planningDate", planningDate); // t2
                        orderInfo.put("orderDate", orderRow[2]); // Original order date (from t1 period)

                        // Find employee assignment and update
                        for (Map<String, Object> assignment : employeeAssignments) {
                            if (employeeId.equals(assignment.get("employeeId"))) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> orders = (List<Map<String, Object>>) assignment.get("orders");
                                orders.add(orderInfo);

                                // Update totals
                                assignment.put("totalCards", (Integer) assignment.get("totalCards") + cardCount);
                                assignment.put("totalDuration", (Integer) assignment.get("totalDuration") + durationMinutes);
                                assignment.put("orderCount", orders.size());
                                break;
                            }
                        }

                        log.info(" Unified Planning #{}: Order {} (from {}) -> Employee {} (planned for {})",
                                successCount, orderNumber, orderRow[2], employeeName, planningDate);
                    }

                } catch (Exception orderError) {
                    log.warn(" Failed to assign order {}: {}", i, orderError.getMessage());
                }
            }

            entityManager.flush();

            // Calculate workload percentages
            for (Map<String, Object> assignment : employeeAssignments) {
                Integer totalDuration = (Integer) assignment.get("totalDuration");
                Integer workHoursPerDay = (Integer) assignment.getOrDefault("workHoursPerDay", 8);
                int maxMinutesPerDay = workHoursPerDay * 60;
                double workloadRatio = maxMinutesPerDay > 0 ? (double) totalDuration / maxMinutesPerDay : 0;

                assignment.put("workload", workloadRatio);
                assignment.put("workloadPercentage", Math.round(workloadRatio * 100));
                assignment.put("estimatedHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0);

                // Status based on workload
                String status = workloadRatio >= 1.0 ? "overloaded" :
                        workloadRatio >= 0.8 ? "busy" : "available";
                assignment.put("status", status);
                assignment.put("available", workloadRatio < 0.8);
            }

            result.put("success", true);
            result.put("message", String.format(" UNIFIED SUCCESS: %d orders (from %s) assigned for %s",
                    successCount, startDate, planningDate));
            result.put("employeeAssignments", employeeAssignments);
            result.put("totalOrdersAssigned", successCount);
            result.put("totalOrdersAnalyzed", orderResults.size());
            result.put("employeeCount", employees.size());
            result.put("algorithm", "UNIFIED_ROUND_ROBIN");
            result.put("startDate", startDate); // t1
            result.put("planningDate", planningDate); // t2

            log.info(" UNIFIED SUCCESS: {} orders (from {}) assigned to {} employees (for {})",
                    successCount, startDate, employees.size(), planningDate);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Unified planning failed: {}", e.getMessage(), e);

            result.put("success", false);
            result.put("message", "Unified planning failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());

            return ResponseEntity.ok(result);
        }
    }
    /**
     * Map priority values to database ENUM values
     * @param orderPriority Priority from Order or any source
     * @return Database-compatible priority string
     */
    private String mapPriorityToDatabase(String orderPriority) {
        if (orderPriority == null) return "CLASSIC";

        return switch (orderPriority.toUpperCase()) {
            case "EXCELSIOR" -> "EXCELSIOR";
            case "FAST_PLUS", "FAST+" -> "FAST+";
            case "FAST" -> "FAST";
            case "CLASSIC" -> "CLASSIC";
            default -> "CLASSIC";
        };
    }

}