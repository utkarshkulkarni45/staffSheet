package com.staffsheet.leave.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveTypeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @NotNull(message = "Yearly quota is required")
        @Min(value = 0, message = "Yearly quota must not be negative")
        Integer yearlyQuota,

        boolean carryForward
) {}
