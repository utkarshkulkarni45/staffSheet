package com.staffsheet.employee.dto;

import com.staffsheet.employee.entity.Role;

public record HierarchyFlatResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        Long managerId,
        int depth
) {}

