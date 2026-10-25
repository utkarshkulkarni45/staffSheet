package com.staffsheet.employee.dto;

import com.staffsheet.employee.entity.Role;

import java.util.List;

public record HierarchyNodeResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        int level,
        List<HierarchyNodeResponse> directReports
) {}

