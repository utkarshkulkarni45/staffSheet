package com.staffsheet.employee.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.common.exception.ResourceNotFoundException;
import com.staffsheet.employee.dto.AssignManagerRequest;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.dto.EmployeeResponse;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.mapper.EmployeeMapper;
import com.staffsheet.employee.repository.EmployeeRepository;
import com.staffsheet.leave.service.LeaveBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee empA;
    private Employee empB;
    private Employee empC;

    @BeforeEach
    void setUp() {
        empA = Employee.builder()
                .name("Alice")
                .email("alice@staffsheet.com")
                .passwordHash("hash")
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .joinDate(LocalDate.now())
                .active(true)
                .build();
        empA.setId(1L);

        empB = Employee.builder()
                .name("Bob")
                .email("bob@staffsheet.com")
                .passwordHash("hash")
                .role(Role.MANAGER)
                .department("Engineering")
                .joinDate(LocalDate.now())
                .active(true)
                .build();
        empB.setId(2L);

        empC = Employee.builder()
                .name("Charlie")
                .email("charlie@staffsheet.com")
                .passwordHash("hash")
                .role(Role.MANAGER)
                .department("Engineering")
                .joinDate(LocalDate.now())
                .active(true)
                .build();
        empC.setId(3L);
    }

    @Test
    @DisplayName("Should successfully create an employee")
    void createEmployee_Success() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Alice", "alice@staffsheet.com", "Password@123", Role.EMPLOYEE, "Engineering", LocalDate.now(), null
        );

        when(employeeRepository.existsByEmail("alice@staffsheet.com")).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(empA);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(employeeRepository.save(any(Employee.class))).thenReturn(empA);
        when(employeeMapper.toResponse(empA)).thenReturn(new EmployeeResponse(
                1L, "Alice", "alice@staffsheet.com", Role.EMPLOYEE, "Engineering", LocalDate.now(), true, null, null, 0L, null, null
        ));

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("alice@staffsheet.com");
        verify(employeeRepository).save(any(Employee.class));
        verify(leaveBalanceService).initializeBalancesForEmployee(any(Employee.class), anyInt());
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void createEmployee_DuplicateEmail_ThrowsBadRequest() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Alice", "alice@staffsheet.com", "Password@123", Role.EMPLOYEE, "Engineering", LocalDate.now(), null
        );

        when(employeeRepository.existsByEmail("alice@staffsheet.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject self-management assignment")
    void assignManager_SelfManagement_ThrowsBadRequest() {
        AssignManagerRequest request = new AssignManagerRequest(1L);
        when(employeeRepository.findWithManagerById(1L)).thenReturn(Optional.of(empA));

        assertThatThrownBy(() -> employeeService.assignManager(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An employee cannot be their own manager");
    }

    @Test
    @DisplayName("Should detect and prevent cyclic manager assignment: A -> B -> A")
    void assignManager_DirectCycle_ThrowsBadRequest() {
        // empB currently reports to empA (empB.manager = empA)
        empB.setManager(empA);

        AssignManagerRequest request = new AssignManagerRequest(2L); // Trying to set empA.manager = empB

        when(employeeRepository.findWithManagerById(1L)).thenReturn(Optional.of(empA));
        when(employeeRepository.findWithManagerById(2L)).thenReturn(Optional.of(empB));

        assertThatThrownBy(() -> employeeService.assignManager(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cyclic manager assignment detected");
    }

    @Test
    @DisplayName("Should detect and prevent transitive cyclic assignment: A -> B -> C -> A")
    void assignManager_TransitiveCycle_ThrowsBadRequest() {
        // Chain: empC reports to empB, empB reports to empA
        empB.setManager(empA);
        empC.setManager(empB);

        AssignManagerRequest request = new AssignManagerRequest(3L); // Trying to set empA.manager = empC

        when(employeeRepository.findWithManagerById(1L)).thenReturn(Optional.of(empA));
        when(employeeRepository.findWithManagerById(3L)).thenReturn(Optional.of(empC));
        when(employeeRepository.findWithManagerById(2L)).thenReturn(Optional.of(empB));

        assertThatThrownBy(() -> employeeService.assignManager(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cyclic manager assignment detected");
    }
}

