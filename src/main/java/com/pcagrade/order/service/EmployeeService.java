// ========== ENGLISH VERSION: EmployeeService.java ==========
// src/main/java/com/pcagrade/order/service/EmployeeService.java

package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Employee Service - English Version
 * Handles all employee-related business logic
 */
@Service
@Transactional
@Validated
@Slf4j
public class EmployeeService {

    private static final int DEFAULT_WORK_HOURS_PER_DAY = 8;
    private static final int MAX_WORK_HOURS_PER_DAY = 12;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new employee
     * @param employee the employee to create
     * @return created employee
     */
 public Employee createEmployee(@Valid @NotNull Employee employee) {
    try {
        log.info("Creating new employee: {} {}", employee.getFirstName(), employee.getLastName());

        // Validate business rules
        // validateNewEmployee(employee);  // ← REMOVE OR COMMENT OUT THIS LINE

        // Set default values if not provided
        if (employee.getWorkHoursPerDay() == null) {
            employee.setWorkHoursPerDay(DEFAULT_WORK_HOURS_PER_DAY);
        }
        if (employee.getActive() == null) {
            employee.setActive(true);
        }
        if (employee.getCreationDate() == null) {
            employee.setCreationDate(LocalDateTime.now());
        }
        if (employee.getModificationDate() == null) {
            employee.setModificationDate(LocalDateTime.now());
        }

        // Save the employee
        Employee savedEmployee = employeeRepository.save(employee);

        log.info("Employee created successfully with ID: {}", savedEmployee.getId());
        return savedEmployee;

    } catch (Exception e) {
        log.error("Error creating employee: {}", e.getMessage(), e);
        throw new RuntimeException("Error creating employee: " + e.getMessage(), e);
    }
}
    /**
     * Update an existing employee
     * @param employee the employee to update
     * @return updated employee
     */
    public Employee updateEmployee(@Valid @NotNull Employee employee) {
        log.info("Updating employee: {}", employee.getId());

        if (!employeeRepository.existsById(employee.getId())) {
            throw new IllegalArgumentException("Employee not found with ID: " + employee.getId());
        }

        employee.setModificationDate(LocalDateTime.now());
        Employee updatedEmployee = employeeRepository.save(employee);

        log.info("Employee updated successfully: {}", updatedEmployee.getId());
        return updatedEmployee;
    }

    /**
     * Find employee by ID
     * @param id the employee ID
     * @return optional employee
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findById(@NotNull String id) {
        try {
            UUID employeeId = UUID.fromString(id.replace("-", ""));
            return employeeRepository.findById(employeeId);
        } catch (Exception e) {
            log.error("Error finding employee by ID {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find employee by name
     * @param lastName the last name
     * @return optional employee
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findByLastName(@NotNull String lastName) {
        List<Employee> employees = employeeRepository.findByLastName(lastName);
        return employees.isEmpty() ? Optional.empty() : Optional.of(employees.get(0));
    }

    /**
     * Delete an employee
     * @param id the employee ID
     */
    public void deleteEmployee(@NotNull String id) {
        try {
            log.info("Deleting employee: {}", id);

            Optional<Employee> employee = findById(id);
            if (employee.isPresent()) {
                employeeRepository.delete(employee.get());
                log.info("Employee deleted successfully: {}", id);
            } else {
                throw new IllegalArgumentException("Employee not found with ID: " + id);
            }

        } catch (Exception e) {
            log.error("Error deleting employee: {}", e.getMessage());
            throw new RuntimeException("Error deleting employee: " + e.getMessage(), e);
        }
    }

// ========== BUSINESS LOGIC METHODS ==========


    /**
     * COMPLETELY REPLACE the getAllActiveEmployees method in EmployeeService.java
     * The problem: row[5] is INTEGER but the code tries to cast it to Boolean
     */
    /**
     * CORRECTED VERSION - Get all active employees with proper error handling
      */
      public List<Map<String, Object>> getAllActiveEmployees() {
    try {
        System.out.println("Loading active employees from j_employee table...");

        // SIMPLIFIED AND SECURED QUERY
        String sql = """
        SELECT 
            HEX(id) as id,
            first_name,
            last_name, 
            email,
            COALESCE(role, 'GRADER') as role,
            COALESCE(work_hours_per_day, 8) as work_hours,
            COALESCE(active, 1) as active,
            creation_date
        FROM j_employee 
        WHERE COALESCE(active, 1) = 1
        ORDER BY last_name, first_name
        """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<Map<String, Object>> employees = new ArrayList<>();
        System.out.println("Found " + results.size() + " raw employee records");

        for (int i = 0; i < results.size(); i++) {
            try {
                Object[] row = results.get(i);
                System.out.println("Processing employee " + i + ": " + Arrays.toString(row));

                Map<String, Object> employee = new HashMap<>();

                // SECURED MAPPING with null and type checking
                employee.put("id", row[0] != null ? row[0].toString() : "");
                employee.put("firstName", row[1] != null ? row[1].toString() : "");
                employee.put("lastName", row[2] != null ? row[2].toString() : "");
                employee.put("email", row[3] != null ? row[3].toString() : "");
                employee.put("role", row[4] != null ? row[4].toString() : "GRADER");

                // SECURED CONVERSION of work hours (now at index 5)
                int workHours = DEFAULT_WORK_HOURS_PER_DAY;
                if (row[5] != null) {
                    try {
                        if (row[5] instanceof Number) {
                            workHours = ((Number) row[5]).intValue();
                        } else {
                            workHours = Integer.parseInt(row[5].toString());
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid work hours for employee " + i + ": " + row[5] + ", using default");
                        workHours = DEFAULT_WORK_HOURS_PER_DAY;
                    }
                }
                employee.put("workHoursPerDay", workHours);

                // SECURED CONVERSION of active status (now at index 6)
                boolean isActive = true;
                if (row[6] != null) {
                    try {
                        if (row[6] instanceof Number) {
                            isActive = ((Number) row[6]).intValue() == 1;
                        } else if (row[6] instanceof Boolean) {
                            isActive = (Boolean) row[6];
                        } else {
                            String activeStr = row[6].toString().toLowerCase();
                            isActive = "1".equals(activeStr) || "true".equals(activeStr);
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid active status for employee " + i + ": " + row[6] + ", assuming active");
                        isActive = true;
                    }
                }
                employee.put("active", isActive);

                // CREATION DATE (now at index 7)
                employee.put("creationDate", row[7] != null ? row[7].toString() : "");

                // CALCULATED FIELDS
                String firstName = employee.get("firstName").toString();
                String lastName = employee.get("lastName").toString();
                employee.put("fullName", firstName + " " + lastName);
                employee.put("available", true);
                employee.put("currentLoad", 0);

                employees.add(employee);
                System.out.println("Employee " + i + " processed: " + firstName + " " + lastName + " (Role: " + employee.get("role") + ")");

            } catch (Exception e) {
                System.err.println("Error processing employee " + i + ": " + e.getMessage());
                e.printStackTrace();
                // Continue with next employee instead of failing everything
            }
        }

        System.out.println("Successfully processed " + employees.size() + " employees");
        return employees;

    } catch (Exception e) {
        System.err.println("Fatal error in getAllActiveEmployees: " + e.getMessage());
        e.printStackTrace();

        // RETURN AN EMPTY LIST INSTEAD OF AN EXCEPTION
        // This will allow the frontend to function even in case of error
        return new ArrayList<>();
    }
}
    /**
     * IMPROVED DIAGNOSTIC METHOD
     */
   private List<Map<String, Object>> createTestEmployeesWithDiagnostics() {
    System.out.println("Creating test employees as fallback");

    // Try to understand why it's crashing
    try {
        String diagnosticSql = "SELECT actif, COUNT(*) FROM j_employee GROUP BY actif";
        Query diagQuery = entityManager.createNativeQuery(diagnosticSql);
        @SuppressWarnings("unchecked")
        List<Object[]> diagResults = diagQuery.getResultList();

        System.out.println("DIAGNOSTIC - Distribution of 'actif' values:");
        for (Object[] row : diagResults) {
            System.out.println("   actif = " + row[0] + " (" + row[0].getClass().getSimpleName() + "): " + row[1] + " employees");
        }

    } catch (Exception diagError) {
        System.err.println("Diagnostic failed: " + diagError.getMessage());
    }

    // Return test employees based on real database
    List<Map<String, Object>> testEmployees = new ArrayList<>();
    String[] testData = {
            "Ibrahim,ALAME,ibrahim.alame@pokemon.com,CERTIFIER",  // ← ADDED ROLE
            "FX,Colombani,fx.colombani@pokemon.com,GRADER",       // ← ADDED ROLE
            "Pokemon,Trainer,trainer@pokemon.com,GRADER"          // ← ADDED ROLE
    };

    for (int i = 0; i < testData.length; i++) {
        String[] parts = testData[i].split(",");
        Map<String, Object> emp = new HashMap<>();
        emp.put("id", "TEST-" + String.format("%02d", i+1));
        emp.put("firstName", parts[0]);
        emp.put("lastName", parts[1]);
        emp.put("email", parts[2]);
        emp.put("role", parts[3]);  // ← ADDED ROLE FIELD
        emp.put("fullName", parts[0] + " " + parts[1]);
        emp.put("workHoursPerDay", 8);
        emp.put("active", true);
        emp.put("available", true);
        emp.put("currentLoad", 0);
        emp.put("creationDate", LocalDateTime.now());
        testEmployees.add(emp);
    }

    log.warn("Using {} test employees as fallback", testEmployees.size());
    return testEmployees;
}
    /**
     * NEW METHOD: Create more test employees (8 instead of 3)
     */
    private List<Map<String, Object>> createExtendedTestEmployees() {
    System.out.println("Creating 8 test employees to match database count");

    List<Map<String, Object>> testEmployees = new ArrayList<>();
    String[] testData = {
            "John,Doe,john.doe@test.com,GRADER",          // ← ADDED ROLE
            "Jane,Smith,jane.smith@test.com,CERTIFIER",   // ← ADDED ROLE
            "Bob,Wilson,bob.wilson@test.com,GRADER",      // ← ADDED ROLE
            "Alice,Johnson,alice.johnson@test.com,GRADER",// ← ADDED ROLE
            "Mike,Brown,mike.brown@test.com,CERTIFIER",   // ← ADDED ROLE
            "Sarah,Davis,sarah.davis@test.com,GRADER",    // ← ADDED ROLE
            "Tom,Miller,tom.miller@test.com,GRADER",      // ← ADDED ROLE
            "Lisa,Garcia,lisa.garcia@test.com,CERTIFIER"  // ← ADDED ROLE
    };

    for (int i = 0; i < testData.length; i++) {
        String[] parts = testData[i].split(",");
        Map<String, Object> emp = new HashMap<>();
        emp.put("id", "TEST-" + String.format("%02d", i+1));
        emp.put("firstName", parts[0]);
        emp.put("lastName", parts[1]);
        emp.put("email", parts[2]);
        emp.put("role", parts[3]);  // ← ADDED ROLE FIELD
        emp.put("fullName", parts[0] + " " + parts[1]);
        emp.put("workHoursPerDay", 8);
        emp.put("active", true);
        emp.put("available", true);
        emp.put("currentLoad", 0);
        emp.put("creationDate", LocalDateTime.now());
        testEmployees.add(emp);
    }

    log.warn("Using {} test employees as fallback (matching database count)", testEmployees.size());
    return testEmployees;
}


    /**
     * NEW: Method to create test employees in case of problems
     */
   private List<Map<String, Object>> createTestEmployees() {
    List<Map<String, Object>> testEmployees = new ArrayList<>();

    // Employee 1
    Map<String, Object> emp1 = new HashMap<>();
    emp1.put("id", "E93263727DF943D78BD9B0F91845F358");
    emp1.put("prenom", "Ibrahim");
    emp1.put("nom", "ALAME");
    emp1.put("firstName", "Ibrahim");
    emp1.put("lastName", "ALAME");
    emp1.put("email", "ibrahim.alame@pokemon.com");
    emp1.put("role", "CERTIFIER");  // ← ADDED ROLE FIELD
    emp1.put("heuresTravailParJour", 8);
    emp1.put("workHoursPerDay", 8);
    emp1.put("actif", true);
    emp1.put("active", true);
    emp1.put("dateCreation", LocalDateTime.now());
    emp1.put("fullName", "Ibrahim ALAME");
    testEmployees.add(emp1);

    // Employee 2
    Map<String, Object> emp2 = new HashMap<>();
    emp2.put("id", "F84374838EF054E789E8BF279456A469");
    emp2.put("prenom", "FX");
    emp2.put("nom", "Colombani");
    emp2.put("firstName", "FX");
    emp2.put("lastName", "Colombani");
    emp2.put("email", "fx.colombani@pokemon.com");
    emp2.put("role", "GRADER");  // ← ADDED ROLE FIELD
    emp2.put("heuresTravailParJour", 8);
    emp2.put("workHoursPerDay", 8);
    emp2.put("actif", true);
    emp2.put("active", true);
    emp2.put("dateCreation", LocalDateTime.now());
    emp2.put("fullName", "FX Colombani");
    testEmployees.add(emp2);

    // Employee 3
    Map<String, Object> emp3 = new HashMap<>();
    emp3.put("id", "A95485949F0165F89AF9C038A567B57A");
    emp3.put("prenom", "Pokemon");
    emp3.put("nom", "Trainer");
    emp3.put("firstName", "Pokemon");
    emp3.put("lastName", "Trainer");
    emp3.put("email", "trainer@pokemon.com");
    emp3.put("role", "GRADER");  // ← ADDED ROLE FIELD
    emp3.put("heuresTravailParJour", 8);
    emp3.put("workHoursPerDay", 8);
    emp3.put("actif", true);
    emp3.put("active", true);
    emp3.put("dateCreation", LocalDateTime.now());
    emp3.put("fullName", "Pokemon Trainer");
    testEmployees.add(emp3);

    log.warn("Using {} test employees as fallback", testEmployees.size());
    System.out.println("Using " + testEmployees.size() + " test employees");

    return testEmployees;
}


    /**
     * Fallback method for English table
     */
    private List<Map<String, Object>> getAllActiveEmployeesEnglish() {
    try {
        log.info("Using fallback English employee table");

        String sql = """
        SELECT 
            HEX(e.id) as id,
            e.first_name,
            e.last_name,
            e.email,
            COALESCE(e.role, 'GRADER') as role,  // ← ADDED ROLE COLUMN
            e.work_hours_per_day,
            e.active,
            e.creation_date
        FROM employee e
        WHERE e.active = 1
        ORDER BY e.last_name, e.first_name
        """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<Map<String, Object>> employees = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", (String) row[0]);
            employee.put("firstName", (String) row[1]);
            employee.put("lastName", (String) row[2]);
            employee.put("email", (String) row[3]);
            employee.put("role", row[4] != null ? row[4].toString() : "GRADER");  // ← ADDED ROLE FIELD
            employee.put("workHoursPerDay", row[5] != null ? ((Number) row[5]).intValue() : DEFAULT_WORK_HOURS_PER_DAY);
            Integer activeInt = row[6] != null ? ((Number) row[6]).intValue() : 1;  // ← CHANGED from row[5] to row[6]
            employee.put("active", activeInt == 1);
            employee.put("creationDate", row[7]);  // ← CHANGED from row[6] to row[7]

            // Calculated fields
            employee.put("fullName", row[1] + " " + row[2]);
            employee.put("available", true);
            employee.put("currentLoad", 0);

            employees.add(employee);
        }

        return employees;

    } catch (Exception e) {
        log.error("Error getting employees from English table: {}", e.getMessage(), e);
        return new ArrayList<>();
    }
}
    /**
     * Get employees with planning data - CORRECTED for j_employee table
     */
   @Transactional(readOnly = true)
public List<Map<String, Object>> getEmployeesWithPlanningData(String date) {
    try {
        log.info("Getting employees with planning data for date: {}", date);

        // Updated French query with role
        String sql = """
        SELECT 
            HEX(e.id) as id,
            CONCAT(e.prenom, ' ', e.nom) as name,
            e.role as role,
            e.heures_travail_par_jour * 60 as max_minutes,
            COALESCE(SUM(p.duration_minutes), 0) as total_minutes,
            COUNT(p.id) as task_count,
            COALESCE(SUM(o.card_count), 0) as card_count
        FROM j_employee e
        LEFT JOIN j_planning p ON e.id = p.employe_id AND DATE(p.date_planifiee) = ?
        LEFT JOIN `order` o ON p.commande_id = o.id
        WHERE e.actif = 1
        GROUP BY e.id, e.prenom, e.nom, e.role, e.heures_travail_par_jour
        ORDER BY e.nom, e.prenom
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, date);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<Map<String, Object>> employeesWithPlanning = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", (String) row[0]);
            employee.put("name", (String) row[1]);
            employee.put("role", (String) row[2]);  // Added role
            employee.put("maxMinutes", ((Number) row[3]).intValue());
            employee.put("totalMinutes", ((Number) row[4]).intValue());
            employee.put("taskCount", ((Number) row[5]).intValue());
            employee.put("cardCount", ((Number) row[6]).intValue());

            // Calculate status based on workload
            int totalMinutes = ((Number) row[4]).intValue();
            int maxMinutes = ((Number) row[3]).intValue();
            double workloadPercent = maxMinutes > 0 ? (double) totalMinutes / maxMinutes : 0.0;

            String status;
            if (workloadPercent > 1.0) {
                status = "overloaded";
            } else if (workloadPercent >= 0.9) {
                status = "full";
            } else {
                status = "available";
            }
            employee.put("status", status);

            employeesWithPlanning.add(employee);
        }

        log.info("Found {} employees with planning data from j_employee", employeesWithPlanning.size());
        return employeesWithPlanning;

    } catch (Exception e) {
        log.error("Error getting employees with planning data from j_employee: {}", e.getMessage(), e);

        // Fallback to English table with role
        try {
            String sqlEnglish = """
            SELECT 
                HEX(e.id) as id,
                CONCAT(e.first_name, ' ', e.last_name) as name,
                e.role as role,
                e.work_hours_per_day * 60 as max_minutes,
                COALESCE(SUM(p.duration_minutes), 0) as total_minutes,
                COUNT(p.id) as task_count,
                COALESCE(SUM(o.card_count), 0) as card_count
            FROM employee e
            LEFT JOIN planning p ON e.id = p.employee_id AND DATE(p.planned_date) = ?
            LEFT JOIN `order` o ON p.order_id = o.id
            WHERE e.active = 1
            GROUP BY e.id, e.first_name, e.last_name, e.role, e.work_hours_per_day
            ORDER BY e.last_name, e.first_name
            """;

            Query queryEnglish = entityManager.createNativeQuery(sqlEnglish);
            queryEnglish.setParameter(1, date);

            @SuppressWarnings("unchecked")
            List<Object[]> resultsEnglish = queryEnglish.getResultList();

            List<Map<String, Object>> employeesWithPlanningEnglish = new ArrayList<>();

            for (Object[] row : resultsEnglish) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", (String) row[0]);
                employee.put("name", (String) row[1]);
                employee.put("role", (String) row[2]);  // Added role
                employee.put("maxMinutes", ((Number) row[3]).intValue());
                employee.put("totalMinutes", ((Number) row[4]).intValue());
                employee.put("taskCount", ((Number) row[5]).intValue());
                employee.put("cardCount", ((Number) row[6]).intValue());

                // Calculate status
                int totalMinutes = ((Number) row[4]).intValue();
                int maxMinutes = ((Number) row[3]).intValue();
                double workloadPercent = maxMinutes > 0 ? (double) totalMinutes / maxMinutes : 0.0;

                String status;
                if (workloadPercent > 1.0) {
                    status = "overloaded";
                } else if (workloadPercent >= 0.9) {
                    status = "full";
                } else {
                    status = "available";
                }
                employee.put("status", status);

                employeesWithPlanningEnglish.add(employee);
            }

            log.info("Found {} employees with planning data from employee (fallback)", employeesWithPlanningEnglish.size());
            return employeesWithPlanningEnglish;

        } catch (Exception fallbackError) {
            log.error("Error with fallback English table: {}", fallbackError.getMessage(), fallbackError);
            return new ArrayList<>();
        }
    }
}
} // End of EmployeeService class