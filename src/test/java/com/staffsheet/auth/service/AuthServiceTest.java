package com.staffsheet.auth.service;

import com.staffsheet.auth.dto.AuthResponse;
import com.staffsheet.auth.dto.LoginRequest;
import com.staffsheet.common.exception.UnauthorizedException;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.repository.EmployeeRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .name("Alice Developer")
                .email("alice@staffsheet.com")
                .passwordHash("encodedPassword")
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .joinDate(LocalDate.of(2025, 1, 15))
                .active(true)
                .build();
        employee.setId(10L);
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void login_Success() {
        LoginRequest request = new LoginRequest("alice@staffsheet.com", "Secret@123");

        when(employeeRepository.findByEmail("alice@staffsheet.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("Secret@123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(10L, "alice@staffsheet.com", "EMPLOYEE")).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("alice@staffsheet.com");
        assertThat(response.role()).isEqualTo("EMPLOYEE");

        verify(employeeRepository).findByEmail("alice@staffsheet.com");
        verify(passwordEncoder).matches("Secret@123", "encodedPassword");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when password does not match")
    void login_InvalidPassword_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("alice@staffsheet.com", "WrongPassword");

        when(employeeRepository.findByEmail("alice@staffsheet.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when employee account is deactivated")
    void login_DeactivatedAccount_ThrowsUnauthorized() {
        employee.setActive(false);
        LoginRequest request = new LoginRequest("alice@staffsheet.com", "Secret@123");

        when(employeeRepository.findByEmail("alice@staffsheet.com")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Employee account is deactivated");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when employee email not found")
    void login_UserNotFound_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("unknown@staffsheet.com", "Secret@123");

        when(employeeRepository.findByEmail("unknown@staffsheet.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }
}

