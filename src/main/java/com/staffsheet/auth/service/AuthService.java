package com.staffsheet.auth.service;

import com.staffsheet.auth.dto.AuthResponse;
import com.staffsheet.auth.dto.LoginRequest;
import com.staffsheet.common.exception.UnauthorizedException;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!employee.isActive()) {
            throw new UnauthorizedException("Employee account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        return AuthResponse.of(
                token,
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );
    }
}

