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
     * GET ALL EMPLOYEES FOR FRONTEND
     * Endpoint: GET /api/employees
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        try {
            System.out.println("Frontend: Retrieving employees list...");

            // Get real employees from database
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();

            System.out.println("" + employees.size() + " employees returned from database");
            return ResponseEntity.ok(employees);

        } catch (Exception e) {
            System.err.println("Error retrieving employees: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * GET ACTIVE EMPLOYEES ONLY
     * Endpoint: GET /api/employees/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveEmployees() {
        try {
            System.out.println("Frontend: Getting active employees from database...");

            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();

            System.out.println("" + employees.size() + " active employees returned from database");
            return ResponseEntity.ok(employees);

        } catch (Exception e) {
            System.err.println("Error getting active employees: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * CREATE EMPLOYEE - FIXED POST ENDPOINT
     * Endpoint: POST /api/employees
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody Map<String, Object> employeeData) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("Creating employee with data: " + employeeData);

            // 1. Validate input data
            String firstName = (String) employeeData.get("firstName");
            String lastName = (String) employeeData.get("lastName");
            String email = (String) employeeData.get("email");
            String role = (String) employeeData.get("role");
            String phone = (String) employeeData.get("phone");

            if (firstName == null || firstName.trim().isEmpty() ||
                    lastName == null || lastName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "First name and last name are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Set default role if not provided
            if (role == null || role.trim().isEmpty()) {
                role = "GRADER";
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

            // 4. Insert into j_employee table with role
            String insertSql = """
                INSERT INTO j_employee 
                            (id, first_name, last_name, email, phone, role, work_hours_per_day, active, creation_date, modification_date)
                            VALUES (UNHEX(?), ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;

            Query insertQuery = entityManager.createNativeQuery(insertSql);
            insertQuery.setParameter(1, employeeId);
            insertQuery.setParameter(2, firstName.trim());
            insertQuery.setParameter(3, lastName.trim());
            insertQuery.setParameter(4, email != null ? email.trim() : null);
            insertQuery.setParameter(5, phone != null ? phone.trim() : null);
            insertQuery.setParameter(6, role);
            insertQuery.setParameter(7, workHours);
            insertQuery.setParameter(8, active ? 1 : 0); // Convert Boolean to Integer

            int rowsAffected = insertQuery.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Employee created successfully: " + firstName + " " + lastName);

                response.put("success", true);
                response.put("message", "Employee created successfully");
                response.put("id", employeeId);
                response.put("firstName", firstName);
                response.put("lastName", lastName);
                response.put("fullName", firstName + " " + lastName);
                response.put("email", email);
                response.put("phone", phone);
                response.put("role", role);
                response.put("workHoursPerDay", workHours);
                response.put("active", active);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to create employee");
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error creating employee: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

   /**
     * GET EMPLOYEE BY ID
     * Endpoint: GET /api/employees/{id}
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeById(@PathVariable String employeeId) {
        try {
            System.out.println("Getting employee by ID: " + employeeId);

            // Clean employee ID (remove dashes if UUID format)
            String cleanEmployeeId = employeeId.replace("-", "");

            String sql = """
                SELECT 
                        HEX(id) as id,
                        first_name as firstName,
                        last_name as lastName,
                    email,
                    phone,
                    role,
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
                employeeData.put("phone", row[4]);
                employeeData.put("role", row[5]);
                employeeData.put("workHoursPerDay", row[6]);
                employeeData.put("active", ((Number) row[7]).intValue() == 1);
                employeeData.put("creationDate", row[8]);
                employeeData.put("modificationDate", row[9]);
                employeeData.put("fullName", row[1] + " " + row[2]);

                return ResponseEntity.ok(employeeData);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("Error getting employee by ID: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * UPDATE EMPLOYEE INCLUDING ROLE
     * Endpoint: PUT /api/employees/{id}
     */
    @PutMapping("/{employeeId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateEmployee(@PathVariable String employeeId, @RequestBody Map<String, Object> employeeData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Updating employee " + employeeId + " with data: " + employeeData);
            
            // Clean employee ID
            String cleanEmployeeId = employeeId.replace("-", "");
            
            // Extract employee data including role
            String firstName = (String) employeeData.get("firstName");
            String lastName = (String) employeeData.get("lastName");
            String email = (String) employeeData.get("email");
            String phone = (String) employeeData.get("phone");
            String role = (String) employeeData.get("role");
            Boolean active = (Boolean) employeeData.get("active");
            Integer workHoursPerDay = (Integer) employeeData.get("workHoursPerDay");
            
            if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "First name and last name are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update employee including role
            String sql = """
                UPDATE j_employee 
                SET first_name = ?, last_name = ?, email = ?, phone = ?, role = ?, 
                    work_hours_per_day = ?, active = ?, modification_date = NOW()
                WHERE HEX(id) = ?
            """;
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, firstName.trim());
            query.setParameter(2, lastName.trim());
            query.setParameter(3, email != null ? email.trim() : null);
            query.setParameter(4, phone != null ? phone.trim() : null);
            query.setParameter(5, role != null ? role : "GRADER");
            query.setParameter(6, workHoursPerDay != null ? workHoursPerDay : 8);
            query.setParameter(7, active != null ? (active ? 1 : 0) : 1);
            query.setParameter(8, cleanEmployeeId);
            
            int result = query.executeUpdate();
            
            if (result > 0) {
                System.out.println("Employee updated successfully with role: " + role);
                response.put("success", true);
                response.put("message", "Employee updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Employee not found with id: " + employeeId);
                return ResponseEntity.status(404).body(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error updating employee: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error updating employee: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * GET EMPLOYEES BY ROLE
     * Endpoint: GET /api/employees/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByRole(@PathVariable String role) {
        try {
            System.out.println("Retrieving employees with role: " + role);
            
            String sql = """
                SELECT 
                    HEX(id) as id,
                    first_name as firstName,
                    last_name as lastName,
                    email,
                    phone,
                    role,
                    work_hours_per_day as workHoursPerDay,
                    active,
                    creation_date as creationDate,
                    modification_date as modificationDate
                FROM j_employee 
                WHERE role = ? AND active = 1
            """;
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, role);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> employees = new ArrayList<>();
            
            for (Object[] result : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", result[0]);
                employee.put("firstName", result[1]);
                employee.put("lastName", result[2]);
                employee.put("email", result[3]);
                employee.put("phone", result[4]);
                employee.put("role", result[5]);
                employee.put("workHoursPerDay", result[6]);
                employee.put("active", ((Number) result[7]).intValue() == 1);
                employee.put("creationDate", result[8]);
                employee.put("modificationDate", result[9]);
                employee.put("fullName", result[1] + " " + result[2]);
                employees.add(employee);
            }
            
            return ResponseEntity.ok(employees);
            
        } catch (Exception e) {
            System.err.println("Error retrieving employees by role: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * INITIALIZE EMPLOYEE TABLE (if needed)
     * Endpoint: POST /api/employees/init-table
     */
    @PostMapping("/init-table")
    @Transactional
    public ResponseEntity<Map<String, Object>> initializeEmployeeTable() {
        try {
            System.out.println("Initializing employee table...");

            String createTableSql = """
                CREATE TABLE IF NOT EXISTS j_employee (
                    id BINARY(16) NOT NULL PRIMARY KEY,
                    first_name VARCHAR(100) NOT NULL,
                    last_name VARCHAR(100) NOT NULL,
                    email VARCHAR(150),
                    role VARCHAR(20) DEFAULT 'GRADER',
                    work_hours_per_day INT DEFAULT 8,
                    active BOOLEAN DEFAULT TRUE,
                    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                    modification_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_employee_active (active),
                    INDEX idx_employee_name (last_name, first_name),
                    INDEX idx_employee_email (email),
                    INDEX idx_employee_role (role)
                )
                """;

            Query query = entityManager.createNativeQuery(createTableSql);
            query.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee table created/verified successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating table: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * CREATE TEST EMPLOYEE (for debugging)
     * Endpoint: POST /api/employees/create-test
     */
    @PostMapping("/create-test")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestEmployee() {
        try {
            System.out.println("Creating test employee...");

            Map<String, Object> testData = new HashMap<>();
            testData.put("firstName", "John");
            testData.put("lastName", "Doe");
            testData.put("email", "john.doe@test.com");
            testData.put("phone", "+1234567890");
            testData.put("role", "GRADER");
            testData.put("workHoursPerDay", 8);

            return createEmployee(testData);

        } catch (Exception e) {
            System.err.println("Error creating test employee: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DEBUG ENDPOINT - Employee diagnostics
     * Endpoint: GET /api/employees/debug
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugEmployees() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("DEBUG: Employee endpoint diagnostic...");

            // 1. Test database connectivity
            debug.put("timestamp", new Date());
            debug.put("database_connected", false);

            try {
                Query testQuery = entityManager.createNativeQuery("SELECT 1");
                testQuery.getSingleResult();
                debug.put("database_connected", true);
                System.out.println("Database connection OK");
            } catch (Exception e) {
                debug.put("database_error", e.getMessage());
                System.out.println("Database connection failed: " + e.getMessage());
            }

            // 2. Test j_employee table structure
            try {
                Query structureQuery = entityManager.createNativeQuery(
                        "DESCRIBE j_employee"
                );
                @SuppressWarnings("unchecked")
                List<Object[]> structure = structureQuery.getResultList();

                List<Map<String, Object>> columns = new ArrayList<>();
                for (Object[] row : structure) {
                    Map<String, Object> col = new HashMap<>();
                    col.put("field", row[0]);
                    col.put("type", row[1]);
                    col.put("null", row[2]);
                    col.put("key", row[3]);
                    col.put("default", row[4]);
                    columns.add(col);
                }
                debug.put("table_structure", columns);
                System.out.println("Table structure retrieved");

            } catch (Exception e) {
                debug.put("structure_error", e.getMessage());
                System.out.println("Cannot get table structure: " + e.getMessage());
            }

            // 3. Count employees
            try {
                Query countQuery = entityManager.createNativeQuery(
                        "SELECT COUNT(*) as total, " +
                                "SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) as active_count " +
                                "FROM j_employee"
                );
                Object[] countResult = (Object[]) countQuery.getSingleResult();

                debug.put("total_employees", countResult[0]);
                debug.put("active_employees", countResult[1]);
                System.out.println("Employee count: total=" + countResult[0] + ", active=" + countResult[1]);

            } catch (Exception e) {
                debug.put("count_error", e.getMessage());
                System.out.println("Cannot count employees: " + e.getMessage());
            }

            // 4. Get some employee examples
            try {
                Query sampleQuery = entityManager.createNativeQuery(
                        "SELECT HEX(id), first_name, last_name, email, phone, role, active " +
                                "FROM j_employee LIMIT 3"
                );
                @SuppressWarnings("unchecked")
                List<Object[]> samples = sampleQuery.getResultList();

                List<Map<String, Object>> sampleEmployees = new ArrayList<>();
                for (Object[] row : samples) {
                    Map<String, Object> emp = new HashMap<>();
                    emp.put("id", row[0]);
                    emp.put("firstName", row[1]);
                    emp.put("lastName", row[2]);
                    emp.put("email", row[3]);
                    emp.put("phone", row[4]);
                    emp.put("role", row[5]);
                    emp.put("active", row[6]);
                    sampleEmployees.add(emp);
                }
                debug.put("sample_employees", sampleEmployees);
                System.out.println("Sample employees retrieved: " + samples.size());

            } catch (Exception e) {
                debug.put("sample_error", e.getMessage());
                System.out.println("Cannot get sample employees: " + e.getMessage());
            }

            // 5. Test EmployeeService method
            try {
                List<Map<String, Object>> serviceResult = employeeService.getAllActiveEmployees();
                debug.put("service_result_count", serviceResult.size());
                debug.put("service_success", true);

                if (!serviceResult.isEmpty()) {
                    debug.put("service_first_employee", serviceResult.get(0));
                }
                System.out.println("EmployeeService returned: " + serviceResult.size() + " employees");

            } catch (Exception e) {
                debug.put("service_error", e.getMessage());
                debug.put("service_success", false);
                debug.put("service_stack_trace", Arrays.toString(e.getStackTrace()));
                System.out.println("EmployeeService failed: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("DEBUG completed, returning results");
            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.out.println("DEBUG endpoint failed: " + e.getMessage());
            e.printStackTrace();

            debug.put("debug_endpoint_error", e.getMessage());
            debug.put("debug_endpoint_success", false);
            return ResponseEntity.status(500).body(debug);
        }
    }
}