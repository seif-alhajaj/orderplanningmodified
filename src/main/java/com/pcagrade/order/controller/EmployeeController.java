package com.pcagrade.order.controller;

import com.pcagrade.order.service.EmployeeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Employee Management - English Version
 * Handles employee CRUD operations and planning-related endpoints
 */
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173"})
public class EmployeeController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 👥 GET ALL EMPLOYEES FOR FRONTEND
     * Endpoint: GET /api/employees
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        try {
            System.out.println("👥 Frontend: Retrieving employees list...");

            // Get real employees from database
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();

            System.out.println("✅ " + employees.size() + " employees returned from database");
            return ResponseEntity.ok(employees);

        } catch (Exception e) {
            System.err.println("❌ Error retrieving employees: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 👥 GET ACTIVE EMPLOYEES ONLY
     * Endpoint: GET /api/employees/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveEmployees() {
        try {
            System.out.println("👥 Frontend: Getting active employees from database...");

            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();

            System.out.println("✅ " + employees.size() + " active employees returned from database");
            return ResponseEntity.ok(employees);

        } catch (Exception e) {
            System.err.println("❌ Error getting active employees: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * ✅ CREATE EMPLOYEE - FIXED POST ENDPOINT
     * Endpoint: POST /api/employees
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody Map<String, Object> employeeData) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("💾 Creating employee with data: " + employeeData);

            // 1. Validate input data
            String firstName = (String) employeeData.get("firstName");
            String lastName = (String) employeeData.get("lastName");
            String email = (String) employeeData.get("email");

            if (firstName == null || firstName.trim().isEmpty() ||
                    lastName == null || lastName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "First name and last name are required");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. Set default values
            Integer workHours = 8;
            if (employeeData.containsKey("workHoursPerDay")) {
                Object workHoursObj = employeeData.get("workHoursPerDay");
                if (workHoursObj instanceof Number) {
                    workHours = ((Number) workHoursObj).intValue();
                }
            }

            Boolean active = true;
            if (employeeData.containsKey("active")) {
                active = (Boolean) employeeData.get("active");
            }

            // 3. Generate UUID for employee ID
            String employeeId = UUID.randomUUID().toString().replace("-", "");

            // 4. Insert into j_employee table
            String insertSql = """
                INSERT INTO j_employee 
                (id, first_name, last_name, email, work_hours_per_day, active, creation_date, modification_date)
                VALUES (UNHEX(?), ?, ?, ?, ?, ?, NOW(), NOW())
            """;

            Query insertQuery = entityManager.createNativeQuery(insertSql);
            insertQuery.setParameter(1, employeeId);
            insertQuery.setParameter(2, firstName.trim());
            insertQuery.setParameter(3, lastName.trim());
            insertQuery.setParameter(4, email != null ? email.trim() : null);
            insertQuery.setParameter(5, workHours);
            insertQuery.setParameter(6, active ? 1 : 0); // Convert Boolean to Integer

            int rowsAffected = insertQuery.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Employee created successfully: " + firstName + " " + lastName);

                response.put("success", true);
                response.put("message", "Employee created successfully");
                response.put("id", employeeId);
                response.put("fullName", firstName + " " + lastName);
                response.put("email", email);
                response.put("workHoursPerDay", workHours);
                response.put("active", active);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to create employee");
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Error creating employee: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error creating employee: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 👤 GET EMPLOYEE BY ID
     * Endpoint: GET /api/employees/{id}
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeById(@PathVariable String employeeId) {
        try {
            System.out.println("👤 Getting employee by ID: " + employeeId);

            // Clean employee ID (remove dashes if UUID format)
            String cleanEmployeeId = employeeId.replace("-", "");

            String sql = """
                SELECT 
                    HEX(id) as id,
                    first_name as firstName,
                    last_name as lastName,
                    email,
                    work_hours_per_day as workHoursPerDay,
                    active,
                    creation_date as creationDate,
                    modification_date as modificationDate
                FROM j_employee
                WHERE HEX(id) = ? AND active = 1
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, cleanEmployeeId);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("id", row[0]);
                employeeData.put("firstName", row[1]);
                employeeData.put("lastName", row[2]);
                employeeData.put("email", row[3]);
                employeeData.put("workHoursPerDay", row[4]);
                employeeData.put("active", ((Number) row[5]).intValue() == 1);
                employeeData.put("creationDate", row[6]);
                employeeData.put("modificationDate", row[7]);
                employeeData.put("fullName", row[1] + " " + row[2]);

                return ResponseEntity.ok(employeeData);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting employee by ID: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔧 INITIALIZE EMPLOYEE TABLE (if needed)
     * Endpoint: POST /api/employees/init-table
     */
    @PostMapping("/init-table")
    @Transactional
    public ResponseEntity<Map<String, Object>> initializeEmployeeTable() {
        try {
            System.out.println("🔧 Initializing employee table...");

            String createTableSql = """
                CREATE TABLE IF NOT EXISTS j_employee (
                    id BINARY(16) NOT NULL PRIMARY KEY,
                    first_name VARCHAR(100) NOT NULL,
                    last_name VARCHAR(100) NOT NULL,
                    email VARCHAR(150),
                    work_hours_per_day INT DEFAULT 8,
                    active BOOLEAN DEFAULT TRUE,
                    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                    modification_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_employee_active (active),
                    INDEX idx_employee_name (last_name, first_name),
                    INDEX idx_employee_email (email)
                )
                """;

            Query query = entityManager.createNativeQuery(createTableSql);
            query.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee table created/verified successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creating table: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating table: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 🧪 CREATE TEST EMPLOYEE (for debugging)
     * Endpoint: POST /api/employees/create-test
     */
    @PostMapping("/create-test")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestEmployee() {
        try {
            System.out.println("🧪 Creating test employee...");

            Map<String, Object> testData = new HashMap<>();
            testData.put("firstName", "John");
            testData.put("lastName", "Doe");
            testData.put("email", "john.doe@test.com");
            testData.put("workHoursPerDay", 8);

            return createEmployee(testData);

        } catch (Exception e) {
            System.err.println("❌ Error creating test employee: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔍 DEBUG ENDPOINT - Check database and table status
     * Endpoint: GET /api/employees/debug
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugEmployees() {
        Map<String, Object> debug = new HashMap<>();

        try {
            // 1. Check if j_employee table exists
            String sqlCheckTable = "SHOW TABLES LIKE 'j_employee'";
            Query queryCheckTable = entityManager.createNativeQuery(sqlCheckTable);
            @SuppressWarnings("unchecked")
            List<Object> tableExists = queryCheckTable.getResultList();

            debug.put("table_j_employee_exists", !tableExists.isEmpty());

            if (!tableExists.isEmpty()) {
                // 2. Count total and active employees
                String sqlCount = "SELECT COUNT(*) FROM j_employee";
                Query queryCount = entityManager.createNativeQuery(sqlCount);
                Number totalCount = (Number) queryCount.getSingleResult();
                debug.put("j_employee_total_count", totalCount.intValue());

                String sqlActiveCount = "SELECT COUNT(*) FROM j_employee WHERE active = 1";
                Query queryActiveCount = entityManager.createNativeQuery(sqlActiveCount);
                Number activeCount = (Number) queryActiveCount.getSingleResult();
                debug.put("j_employee_active_count", activeCount.intValue());

                // 3. Sample data
                String sqlSample = "SELECT HEX(id), first_name, last_name, email, active FROM j_employee LIMIT 3";
                Query querySample = entityManager.createNativeQuery(sqlSample);
                @SuppressWarnings("unchecked")
                List<Object[]> sampleData = querySample.getResultList();

                List<Map<String, Object>> employees = new ArrayList<>();
                for (Object[] row : sampleData) {
                    Map<String, Object> emp = new HashMap<>();
                    emp.put("id", row[0]);
                    emp.put("firstName", row[1]);
                    emp.put("lastName", row[2]);
                    emp.put("email", row[3]);
                    emp.put("active", ((Number) row[4]).intValue() == 1);
                    employees.add(emp);
                }
                debug.put("sample_employees", employees);
            }

            debug.put("status", "Employee controller is working");

        } catch (Exception e) {
            debug.put("error", e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(debug);
    }
}