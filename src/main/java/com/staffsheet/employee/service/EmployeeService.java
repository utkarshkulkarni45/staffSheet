package com.staffsheet.employee.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.common.exception.ResourceNotFoundException;
import com.staffsheet.employee.dto.AssignManagerRequest;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.dto.EmployeeResponse;
import com.staffsheet.employee.dto.HierarchyFlatResponse;
import com.staffsheet.employee.dto.HierarchyNodeResponse;
import com.staffsheet.employee.dto.UpdateEmployeeRequest;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.mapper.EmployeeMapper;
import com.staffsheet.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDirectReports(Long managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager not found with id: " + managerId);
        }
        return employeeRepository.findByManagerId(managerId).stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HierarchyFlatResponse> getUpwardChain(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        List<Object[]> rows = employeeRepository.findUpwardChainNative(employeeId);
        return rows.stream().map(this::mapRowToHierarchyResponse).toList();
    }

    @Transactional(readOnly = true)
    public HierarchyNodeResponse getReportingSubtree(Long managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + managerId);
        }

        List<Object[]> rows = employeeRepository.findDownwardSubtreeNative(managerId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Employee not found with id: " + managerId);
        }

        return buildHierarchyTree(rows, managerId);
    }

    private HierarchyFlatResponse mapRowToHierarchyResponse(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String name = (String) row[1];
        String email = (String) row[2];
        Role role = Role.valueOf((String) row[3]);
        String department = (String) row[4];
        Long managerId = row[5] != null ? ((Number) row[5]).longValue() : null;
        int depth = ((Number) row[6]).intValue();

        return new HierarchyFlatResponse(id, name, email, role, department, managerId, depth);
    }

    private HierarchyNodeResponse buildHierarchyTree(List<Object[]> rows, Long rootId) {
        // Map parent ID to children nodes
        java.util.Map<Long, java.util.List<HierarchyFlatResponse>> childrenMap = new java.util.HashMap<>();
        HierarchyFlatResponse root = null;

        for (Object[] row : rows) {
            HierarchyFlatResponse node = mapRowToHierarchyResponse(row);
            if (node.id().equals(rootId)) {
                root = node;
            } else if (node.managerId() != null) {
                childrenMap.computeIfAbsent(node.managerId(), k -> new java.util.ArrayList<>()).add(node);
            }
        }

        if (root == null) {
            throw new ResourceNotFoundException("Root manager not found with id: " + rootId);
        }

        return buildRecursiveNode(root, childrenMap);
    }

    private HierarchyNodeResponse buildRecursiveNode(
            HierarchyFlatResponse current,
            java.util.Map<Long, java.util.List<HierarchyFlatResponse>> childrenMap) {
        java.util.List<HierarchyFlatResponse> children = childrenMap.getOrDefault(current.id(), java.util.List.of());

        java.util.List<HierarchyNodeResponse> childNodes = children.stream()
                .map(child -> buildRecursiveNode(child, childrenMap))
                .toList();

        return new HierarchyNodeResponse(
                current.id(),
                current.name(),
                current.email(),
                current.role(),
                current.department(),
                current.depth(),
                childNodes
        );
    }
}

