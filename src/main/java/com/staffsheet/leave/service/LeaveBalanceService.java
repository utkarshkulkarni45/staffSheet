package com.staffsheet.leave.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.common.exception.ResourceNotFoundException;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.repository.EmployeeRepository;
import com.staffsheet.leave.dto.LeaveBalanceResponse;
import com.staffsheet.leave.entity.LeaveBalance;
import com.staffsheet.leave.entity.LeaveType;
import com.staffsheet.leave.mapper.LeaveBalanceMapper;
import com.staffsheet.leave.repository.LeaveBalanceRepository;
import com.staffsheet.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private static final int MIN_SUPPORTED_YEAR = 2000;
    private static final int MAX_SUPPORTED_YEAR = 2100;

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceMapper leaveBalanceMapper;

    /**
     * Idempotently creates one balance per leave type for the employee and year.
     * Existing rows are left untouched so allocations are never silently reset.
     */
    @Transactional
    public void initializeBalancesForEmployee(Employee employee, int year) {
        validateYear(year);

        List<LeaveType> leaveTypes = leaveTypeRepository.findAllByOrderByNameAsc();
        if (leaveTypes.isEmpty()) {
            return;
        }

        Set<Long> coveredTypeIds = new HashSet<>();
        for (LeaveBalance existing : leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), year)) {
            coveredTypeIds.add(existing.getLeaveType().getId());
        }

        List<LeaveBalance> missing = leaveTypes.stream()
                .filter(type -> !coveredTypeIds.contains(type.getId()))
                .map(type -> LeaveBalance.builder()
                        .employee(employee)
                        .leaveType(type)
                        .year(year)
                        .allocated(type.getYearlyQuota())
                        .used(0)
                        .pending(0)
                        .build())
                .toList();

        if (!missing.isEmpty()) {
            leaveBalanceRepository.saveAll(missing);
        }
    }

    /**
     * Backfills missing balances on first read so a new year or a newly added
     * leave type is reflected without a separate provisioning endpoint.
     */
    @Transactional
    public List<LeaveBalanceResponse> getBalancesForEmployee(Long employeeId, int year) {
        validateYear(year);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        initializeBalancesForEmployee(employee, year);

        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .sorted(Comparator.comparing(balance -> balance.getLeaveType().getName()))
                .map(balance -> leaveBalanceMapper.toResponse(balance, availableDays(balance)))
                .toList();
    }

    private int availableDays(LeaveBalance balance) {
        return balance.getAllocated() - balance.getUsed() - balance.getPending();
    }

    private void validateYear(int year) {
        if (year < MIN_SUPPORTED_YEAR || year > MAX_SUPPORTED_YEAR) {
            throw new BadRequestException("Year must be between " + MIN_SUPPORTED_YEAR + " and " + MAX_SUPPORTED_YEAR);
        }
    }
}
