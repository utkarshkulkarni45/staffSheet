package com.staffsheet.leave.dto;

public record LeaveBalanceResponse(
        Long id,
        Long leaveTypeId,
        String leaveTypeName,
        int year,
        int allocated,
        int used,
        int pending,
        int available,
        Long version
) {}
