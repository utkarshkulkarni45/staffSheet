package com.staffsheet.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffsheet.employee.dto.AssignManagerRequest;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();

        employee1 = employeeRepository.save(Employee.builder()
                .name("Alice Leader")
                .email("alice@staffsheet.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.MANAGER)
                .department("Engineering")
                .joinDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .build());

        employee2 = employeeRepository.save(Employee.builder()
                .name("Bob Staff")
                .email("bob@staffsheet.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .joinDate(LocalDate.of(2024, 3, 1))
                .active(true)
                .manager(employee1)
                .build());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/employees creates employee when invoked by HR")
    void createEmployee_ByHR_ReturnsCreated() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Charlie Dev", "charlie@staffsheet.com", "SecurePass@123",
                Role.EMPLOYEE, "Engineering", LocalDate.of(2025, 2, 1), employee1.getId()
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("charlie@staffsheet.com"))
                .andExpect(jsonPath("$.managerId").value(employee1.getId()));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/v1/employees returns 403 Forbidden when invoked by normal EMPLOYEE")
    void createEmployee_ByEmployee_ReturnsForbidden() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Dave Dev", "dave@staffsheet.com", "SecurePass@123",
                Role.EMPLOYEE, "Engineering", LocalDate.of(2025, 2, 1), null
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/employees/{id}/manager rejects self-management with 400")
    void assignManager_SelfManagement_ReturnsBadRequest() throws Exception {
        AssignManagerRequest request = new AssignManagerRequest(employee1.getId());

        mockMvc.perform(put("/api/v1/employees/" + employee1.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An employee cannot be their own manager"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/employees/{id}/manager rejects cyclic assignment with 400")
    void assignManager_CyclicRelation_ReturnsBadRequest() throws Exception {
        // employee2 already reports to employee1. Now trying to assign employee2 as employee1's manager!
        AssignManagerRequest request = new AssignManagerRequest(employee2.getId());

        mockMvc.perform(put("/api/v1/employees/" + employee1.getId() + "/manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Cyclic manager assignment detected")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /api/v1/employees/{id} returns employee details for authenticated user")
    void getEmployeeById_Authenticated_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + employee1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@staffsheet.com"))
                .andExpect(jsonPath("$.name").value("Alice Leader"));
    }
}

