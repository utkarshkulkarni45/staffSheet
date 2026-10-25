package com.staffsheet.employee.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.common.exception.ResourceNotFoundException;
import com.staffsheet.employee.dto.AssignManagerRequest;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.dto.EmployeeResponse;
import com.staffsheet.employee.dto.UpdateEmployeeRequest;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.mapper.EmployeeMapper;
import com.staffsheet.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Employee with email '" + request.email() + "' already exists");
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));

        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.managerId()));
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findWithManagerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(employeeMapper::toResponse);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findWithManagerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setName(request.name());
        employee.setRole(request.role());
        employee.setDepartment(request.department());
        employee.setActive(request.active());

        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toResponse(updated);
    }

    @Transactional
    public EmployeeResponse assignManager(Long employeeId, AssignManagerRequest request) {
        Employee employee = employeeRepository.findWithManagerById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (request.managerId() == null) {
            employee.setManager(null);
            return employeeMapper.toResponse(employeeRepository.save(employee));
        }

        Long newManagerId = request.managerId();

        // Rule: Prevent self-management
        if (employeeId.equals(newManagerId)) {
            throw new BadRequestException("An employee cannot be their own manager");
        }

        Employee newManager = employeeRepository.findWithManagerById(newManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + newManagerId));

        // Rule: Prevent cyclical reporting relationships (e.g., A -> B -> A)
        validateNoCycle(employeeId, newManager);

        employee.setManager(newManager);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    private void validateNoCycle(Long employeeId, Employee targetManager) {
        Set<Long> visited = new HashSet<>();
        Employee current = targetManager;

        while (current != null) {
            if (current.getId().equals(employeeId)) {
                throw new BadRequestException("Cyclic manager assignment detected: Employee ID " + employeeId +
                        " is already an ancestor in the management chain of ID " + targetManager.getId());
            }

            if (!visited.add(current.getId())) {
                break; // safeguard against any pre-existing loops
            }

            current = current.getManager() != null ?
                    employeeRepository.findWithManagerById(current.getManager().getId()).orElse(null) : null;
        }
    }
}

