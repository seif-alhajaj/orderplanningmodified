package com.pcagrade.order.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * EMPLOYEES PLANNING CONTROLLER - Conflict-free version
 * Endpoints dedicated to the enhanced frontend with unique prefixes
 */
@RestController
@RequestMapping("/api/frontend/employees")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class EmployeesPlanningController {

    private static final Logger log = LoggerFactory.getLogger(EmployeesPlanningController.class);

    @Autowired
    private EntityManager entityManager;

    /**
     * GET ALL EMPLOYEES - Management Mode
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmployees() {
        try {
            log.info("Fetching all employees for management view");

            String sql = """
                SELECT 
                    HEX(id) as id,
                    COALESCE(first_name, 'Unknown') as firstName,
                    COALESCE(last_name, 'User') as lastName,
                    COALESCE(email, 'no-email@example.com') as email,
                    COALESCE(phone, '') as phone,
                    COALESCE(role, 'GRADER') as role,
                    COALESCE(active, 1) as active,
                    COALESCE(work_hours_per_day, 8) as workHoursPerDay,
                    creation_date as creationDate,
                    modification_date as modificationDate
                FROM j_employee
                ORDER BY first_name ASC, last_name ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", row[0]);           // id
                employee.put("firstName", row[1]);    // first_name
                employee.put("lastName", row[2]);     // last_name
                employee.put("email", row[3]);        // email
                employee.put("phone", row[4]);        // phone
                employee.put("role", row[5]);         // role
                employee.put("active", ((Number) row[6]).intValue() == 1);  // active
                employee.put("workHoursPerDay", row[7]);  // work_hours_per_day
                employee.put("creationDate", row[8]);     // creation_date
                employee.put("modificationDate", row[9]); // modification_date

                // Computed fields
                employee.put("fullName", row[1] + " " + row[2]);
                employee.put("status", ((Number) row[6]).intValue() == 1 ? "AVAILABLE" : "INACTIVE");

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());

            log.info("Retrieved {} employees", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching employees", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * GET EMPLOYEES WITH PLANNING DATA - Planning Mode
     */
    @GetMapping("/planning-data")
    public ResponseEntity<Map<String, Object>> getEmployeesWithPlanningData(
            @RequestParam(required = false) String date) {

        try {
            log.info("Fetching employees with planning data for date: {}", date);

            String dateFilter = date != null ? " AND p.planning_date = '" + date + "'" : "";

            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email,
                    COALESCE(e.role, 'GRADER') as role,
                    COALESCE(e.active, 1) as active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    COALESCE(SUM(p.estimated_duration_minutes), 0) as totalMinutes,
                    COUNT(p.id) as taskCount,
                    COALESCE(SUM(p.card_count), 0) as cardCount,
                    ROUND(
                        COALESCE(SUM(p.estimated_duration_minutes), 0) / 
                        (COALESCE(e.work_hours_per_day, 8) * 60.0), 2
                    ) as workloadRatio,
                    e.creation_date as creationDate,
                    e.modification_date as modificationDate
                FROM j_employee e
                LEFT JOIN j_planning p ON e.id = p.employee_id""" + dateFilter + """
                GROUP BY e.id, e.first_name, e.last_name, e.email, e.role, e.active, e.work_hours_per_day, 
                         e.creation_date, e.modification_date
                ORDER BY workloadRatio DESC, name ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", row[0]);           // id
                employee.put("name", row[1]);         // computed name
                employee.put("firstName", row[2]);    // first_name
                employee.put("lastName", row[3]);     // last_name
                employee.put("email", row[4]);        // email
                employee.put("role", row[5]);         // role
                employee.put("active", ((Number) row[6]).intValue() == 1);  // active
                employee.put("workHoursPerDay", row[7]);  // work_hours_per_day
                employee.put("totalMinutes", row[8]);     // total_minutes
                employee.put("maxMinutes", ((Number) row[7]).intValue() * 60);
                employee.put("taskCount", row[9]);        // task_count
                employee.put("cardCount", row[10]);       // card_count
                employee.put("creationDate", row[12]);    // creation_date
                employee.put("modificationDate", row[13]);// modification_date

                // Workload calculations
                Number workloadRatioNum = (Number) row[11];
                Double workloadRatio = workloadRatioNum != null ? workloadRatioNum.doubleValue() : 0.0;
                employee.put("workload", workloadRatio);

                // Status determination
                String status;
                boolean available;
                if (workloadRatio >= 1.0) {
                    status = "overloaded";
                    available = false;
                } else if (workloadRatio >= 0.8) {
                    status = "busy";
                    available = false;
                } else {
                    status = "available";
                    available = true;
                }

                employee.put("status", status);
                employee.put("available", available);

                // Additional computed fields
                Integer totalMinutes = ((Number) row[8]).intValue();
                employee.put("estimatedHours", Math.round(totalMinutes / 60.0 * 100.0) / 100.0);
                employee.put("totalCards", row[10]);
                employee.put("activeOrders", row[9]);

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());
            response.put("date", date != null ? date : "all");

            log.info("Retrieved {} employees with planning data", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching employees with planning data", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * GET EMPLOYEE DETAILS - Details of a specific employee
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeDetails(@PathVariable String employeeId) {
        try {
            log.info("Fetching details for employee: {}", employeeId);

            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email,
                    COALESCE(e.role, 'GRADER') as role,
                    COALESCE(e.active, 1) as active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    e.creation_date as creationDate,
                    e.modification_date as modificationDate
                FROM j_employee e
                WHERE HEX(e.id) = ?
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (results.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Employee not found");
                return ResponseEntity.status(404).body(errorResponse);
            }

            Object[] row = results.get(0);
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", row[0]);
            employee.put("name", row[1]);
            employee.put("firstName", row[2]);
            employee.put("lastName", row[3]);
            employee.put("email", row[4]);
            employee.put("role", row[5]);
            employee.put("active", ((Number) row[6]).intValue() == 1);
            employee.put("workHoursPerDay", row[7]);
            employee.put("creationDate", row[8]);
            employee.put("modificationDate", row[9]);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employee);

            log.info("Retrieved details for employee: {}", employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching employee details", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * GET EMPLOYEE ORDERS - Orders assigned to an employee
     */
    @GetMapping("/{employeeId}/orders")
    public ResponseEntity<Map<String, Object>> getEmployeeOrders(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        try {
            log.info("Fetching orders for employee: {}, date: {}", employeeId, date);

            String dateFilter = date != null ? " AND p.planning_date = '" + date + "'" : "";

            String sql = """
                SELECT 
                    HEX(p.id) as planningId,
                    HEX(p.order_id) as orderId,
                    o.num_commande as orderNumber,
                    p.planning_date,
                    p.start_time,
                    p.estimated_duration_minutes,
                    p.priority,
                    p.status,
                    p.completed,
                    p.card_count,
                    p.progress_percentage,
                    ROUND(p.estimated_duration_minutes / 60.0, 2) as estimatedHours
                FROM j_planning p
                LEFT JOIN `order` o ON p.order_id = o.id
                WHERE HEX(p.employee_id) = ?""" + dateFilter + """
                ORDER BY p.planning_date ASC, p.start_time ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();
            int totalDuration = 0;
            int totalCards = 0;

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", row[0]); // Planning ID
                order.put("orderId", row[1]);
                order.put("orderNumber", row[2]);
                order.put("planningDate", row[3]);
                order.put("startTime", row[4]);
                order.put("estimatedDuration", row[5]);
                order.put("priority", row[6]);
                order.put("status", row[7]);
                order.put("completed", row[8]);
                order.put("cardCount", row[9]);
                order.put("progress", row[10]);
                order.put("estimatedHours", row[11]);

                // Add to totals
                Integer duration = (Integer) row[5];
                Integer cardCount = (Integer) row[9];
                if (duration != null) totalDuration += duration;
                if (cardCount != null) totalCards += cardCount;

                orders.add(order);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("total", orders.size());
            response.put("totalDurationMinutes", totalDuration);
            response.put("totalHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0);
            response.put("totalCards", totalCards);

            log.info("Retrieved {} orders for employee {}", orders.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching employee orders", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * GET ORDER CARDS - Cards of a specific order
     */
    @GetMapping("/order/{orderId}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String orderId) {
        try {
            log.info("Fetching cards for order: {}", orderId);

            String sql = """
                SELECT 
                    HEX(cc.id) as id,
                    cc.code_barre,
                    COALESCE(ct.name, CONCAT('Card #', cc.code_barre)) as name,
                    COALESCE(ct.label_name, CONCAT('Label #', cc.code_barre)) as label_name,
                    3 as duration,
                    COALESCE(cc.annotation, 0) as amount
                FROM card_certification_order cco
                INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
                LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
                WHERE HEX(cco.order_id) = ?
                ORDER BY cc.code_barre ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, orderId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("id", row[0]);
                card.put("code_barre", row[1]);
                card.put("name", row[2]);
                card.put("label_name", row[3]);
                card.put("duration", row[4]);
                card.put("amount", row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);

                cards.add(card);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cards", cards);
            response.put("total", cards.size());

            log.info("Retrieved {} cards for order {}", cards.size(), orderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching order cards", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}