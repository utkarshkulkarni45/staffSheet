package com.staffsheet.leave.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.common.exception.ResourceNotFoundException;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.repository.EmployeeRepository;
import com.staffsheet.leave.dto.LeaveBalanceResponse;
import com.staffsheet.leave.entity.LeaveBalance;
import com.staffsheet.leave.entity.LeaveType;
import com.staffsheet.leave.mapper.LeaveBalanceMapper;
import com.staffsheet.leave.repository.LeaveBalanceRepository;
import com.staffsheet.leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    private static final int YEAR = 2026;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveBalanceMapper leaveBalanceMapper;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private Employee employee;
    private LeaveType annual;
    private LeaveType sick;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .name("Alice")
                .email("alice@staffsheet.com")
                .passwordHash("hash")
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .joinDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .build();
        employee.setId(1L);

        annual = LeaveType.builder().name("Annual").yearlyQuota(20).build();
        annual.setId(10L);

        sick = LeaveType.builder().name("Sick").yearlyQuota(10).build();
        sick.setId(11L);
    }

    @Test
    @DisplayName("Should create a balance for every leave type missing for the year")
    void initializeBalancesForEmployee_CreatesMissingBalances() {
        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(annual, sick));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, YEAR)).thenReturn(List.of());

        leaveBalanceService.initializeBalancesForEmployee(employee, YEAR);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LeaveBalance>> captor = ArgumentCaptor.forClass(List.class);
        verify(leaveBalanceRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(balance -> balance.getLeaveType().getName())
                .containsExactlyInAnyOrder("Annual", "Sick");
        assertThat(captor.getValue())
                .extracting(LeaveBalance::getAllocated)
                .containsExactlyInAnyOrder(20, 10);
        assertThat(captor.getValue())
                .allSatisfy(balance -> {
                    assertThat(balance.getYear()).isEqualTo(YEAR);
                    assertThat(balance.getUsed()).isZero();
                    assertThat(balance.getPending()).isZero();
                    assertThat(balance.getEmployee()).isSameAs(employee);
                });
    }

    @Test
    @DisplayName("Should not recreate or reset an existing balance")
    void initializeBalancesForEmployee_SkipsExistingBalances() {
        LeaveBalance existing = LeaveBalance.builder()
                .employee(employee)
                .leaveType(annual)
                .year(YEAR)
                .allocated(18)
                .used(4)
                .pending(1)
                .build();

        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(annual, sick));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, YEAR)).thenReturn(List.of(existing));

        leaveBalanceService.initializeBalancesForEmployee(employee, YEAR);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LeaveBalance>> captor = ArgumentCaptor.forClass(List.class);
        verify(leaveBalanceRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getLeaveType()).isSameAs(sick);
    }

    @Test
    @DisplayName("Should return balances with available = allocated - used - pending")
    void getBalancesForEmployee_ComputesAvailableDays() {
        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(annual)
                .year(YEAR)
                .allocated(20)
                .used(5)
                .pending(3)
                .build();
        LeaveBalanceResponse expected = new LeaveBalanceResponse(1L, 10L, "Annual", YEAR, 20, 5, 3, 12, 0L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(annual));
        when(leaveBalanceRepository.findByEmployeeIdAndYear(1L, YEAR)).thenReturn(List.of(balance));
        when(leaveBalanceMapper.toResponse(balance, 12)).thenReturn(expected);

        List<LeaveBalanceResponse> result = leaveBalanceService.getBalancesForEmployee(1L, YEAR);

        assertThat(result).containsExactly(expected);
        verify(leaveBalanceMapper).toResponse(balance, 12);
    }

    @Test
    @DisplayName("Should throw when the employee does not exist")
    void getBalancesForEmployee_UnknownEmployee_ThrowsNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveBalanceService.getBalancesForEmployee(99L, YEAR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    @DisplayName("Should reject an unsupported balance year")
    void getBalancesForEmployee_InvalidYear_ThrowsBadRequest() {
        assertThatThrownBy(() -> leaveBalanceService.getBalancesForEmployee(1L, 1999))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Year must be between");
    }

    @Test
    @DisplayName("Should do nothing when no leave types are configured")
    void initializeBalancesForEmployee_NoLeaveTypes_DoesNothing() {
        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        leaveBalanceService.initializeBalancesForEmployee(employee, YEAR);

        verify(leaveBalanceRepository, never()).saveAll(any());
    }
}
