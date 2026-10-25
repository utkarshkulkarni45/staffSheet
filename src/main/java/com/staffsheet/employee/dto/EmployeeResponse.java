package com.staffsheet.employee.dto;

import com.staffsheet.employee.entity.Role;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        LocalDate joinDate,
        boolean active,
        Long managerId,
        String managerName,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}

