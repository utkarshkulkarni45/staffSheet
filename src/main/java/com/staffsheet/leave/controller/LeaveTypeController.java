package com.staffsheet.leave.controller;

import com.staffsheet.leave.dto.LeaveTypeRequest;
import com.staffsheet.leave.dto.LeaveTypeResponse;
import com.staffsheet.leave.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-types")
@RequiredArgsConstructor
@Tag(name = "Leave Types", description = "Leave policy administration")
@SecurityRequirement(name = "bearerAuth")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List leave types", description = "Returns all configured leave types")
    public ResponseEntity<List<LeaveTypeResponse>> getAllLeaveTypes() {
        return ResponseEntity.ok(leaveTypeService.getAllLeaveTypes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Create a leave type", description = "Accessible by HR or ADMIN only")
    public ResponseEntity<LeaveTypeResponse> createLeaveType(@Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveTypeService.createLeaveType(request));
    }
}
