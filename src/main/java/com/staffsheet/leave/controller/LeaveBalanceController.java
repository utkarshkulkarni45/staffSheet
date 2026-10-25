package com.staffsheet.leave.controller;

import com.staffsheet.auth.service.CustomUserDetails;
import com.staffsheet.leave.dto.LeaveBalanceResponse;
import com.staffsheet.leave.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/me/balances")
@RequiredArgsConstructor
@Tag(name = "My Leave Balances", description = "Current user's yearly leave balances")
@SecurityRequirement(name = "bearerAuth")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my leave balances",
            description = "Returns this year's balances by default; missing balances are initialized on first read")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveBalanceService.getBalancesForEmployee(currentUser.id(), targetYear));
    }
}
