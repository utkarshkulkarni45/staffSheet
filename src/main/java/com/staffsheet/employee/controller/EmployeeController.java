package com.staffsheet.employee.controller;

import com.staffsheet.employee.dto.AssignManagerRequest;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.dto.EmployeeResponse;
import com.staffsheet.employee.dto.UpdateEmployeeRequest;
import com.staffsheet.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Endpoints for employee CRUD and hierarchy administration")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Create an employee", description = "Accessible by HR or ADMIN only")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get employee by ID", description = "Retrieves employee profile by ID")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Get all employees (paginated)", description = "Retrieves paginated employee directory")
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Update employee details", description = "Updates profile details for an employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Assign or update manager", description = "Reassigns manager with cycle detection")
    public ResponseEntity<EmployeeResponse> assignManager(
            @PathVariable Long id,
            @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(employeeService.assignManager(id, request));
    }

    @GetMapping("/{id}/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get direct reports", description = "Returns immediate direct reports for a manager")
    public ResponseEntity<java.util.List<EmployeeResponse>> getDirectReports(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getDirectReports(id));
    }

    @GetMapping("/{id}/chain")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get upward management chain", description = "Returns reporting chain upward from employee to top executive")
    public ResponseEntity<java.util.List<com.staffsheet.employee.dto.HierarchyFlatResponse>> getUpwardChain(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getUpwardChain(id));
    }

    @GetMapping("/{id}/subtree")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get reporting subtree", description = "Returns full downward hierarchy tree rooted at given manager")
    public ResponseEntity<com.staffsheet.employee.dto.HierarchyNodeResponse> getReportingSubtree(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getReportingSubtree(id));
    }
}

