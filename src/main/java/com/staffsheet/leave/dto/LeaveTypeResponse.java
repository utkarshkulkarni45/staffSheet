package com.staffsheet.leave.dto;

public record LeaveTypeResponse(
        Long id,
        String name,
        int yearlyQuota,
        boolean carryForward,
        Long version
) {}
