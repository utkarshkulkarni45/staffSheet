package com.staffsheet.leave.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffsheet.auth.service.CustomUserDetails;
import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.entity.Employee;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.repository.EmployeeRepository;
import com.staffsheet.leave.dto.LeaveTypeRequest;
import com.staffsheet.leave.entity.Holiday;
import com.staffsheet.leave.entity.LeaveType;
import com.staffsheet.leave.repository.HolidayRepository;
import com.staffsheet.leave.repository.LeaveBalanceRepository;
import com.staffsheet.leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee;

    @BeforeEach
    void setUp() {
        leaveBalanceRepository.deleteAll();
        holidayRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        employeeRepository.deleteAll();

        employee = employeeRepository.save(Employee.builder()
                .name("Alice Staff")
                .email("alice.staff@staffsheet.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .joinDate(LocalDate.of(2025, 1, 1))
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/leave-types creates a leave type for HR")
    void createLeaveType_ByHR_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveTypeRequest("Annual", 20, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Annual"))
                .andExpect(jsonPath("$.yearlyQuota").value(20))
                .andExpect(jsonPath("$.carryForward").value(true));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/v1/leave-types returns 403 for a normal employee")
    void createLeaveType_ByEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveTypeRequest("Annual", 20, false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/v1/leave-types rejects duplicate names with 400")
    void createLeaveType_Duplicate_ReturnsBadRequest() throws Exception {
        leaveTypeRepository.save(LeaveType.builder().name("Annual").yearlyQuota(20).build());

        mockMvc.perform(post("/api/v1/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveTypeRequest("annual", 15, false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /api/v1/leave-types lists leave types sorted by name")
    void getAllLeaveTypes_ReturnsSortedList() throws Exception {
        leaveTypeRepository.save(LeaveType.builder().name("Sick").yearlyQuota(10).build());
        leaveTypeRepository.save(LeaveType.builder().name("Annual").yearlyQuota(20).build());

        mockMvc.perform(get("/api/v1/leave-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Annual"))
                .andExpect(jsonPath("$[1].name").value("Sick"));
    }

    @Test
    @DisplayName("GET /api/v1/me/balances creates missing balances once and returns available days")
    void getMyBalances_InitializesBalancesIdempotently() throws Exception {
        leaveTypeRepository.save(LeaveType.builder().name("Annual").yearlyQuota(20).build());
        int currentYear = Year.now().getValue();

        mockMvc.perform(get("/api/v1/me/balances").with(user(principalFor(employee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Annual"))
                .andExpect(jsonPath("$[0].year").value(currentYear))
                .andExpect(jsonPath("$[0].allocated").value(20))
                .andExpect(jsonPath("$[0].used").value(0))
                .andExpect(jsonPath("$[0].pending").value(0))
                .andExpect(jsonPath("$[0].available").value(20));

        mockMvc.perform(get("/api/v1/me/balances").with(user(principalFor(employee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        assertThat(leaveBalanceRepository.countByEmployeeIdAndYear(employee.getId(), currentYear)).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/v1/me/balances rejects an unsupported year")
    void getMyBalances_InvalidYear_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/me/balances")
                        .param("year", "1999")
                        .with(user(principalFor(employee))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Year must be between")));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("Creating an employee initializes balances for configured leave types")
    void createEmployee_InitializesCurrentYearBalances() throws Exception {
        leaveTypeRepository.save(LeaveType.builder().name("Annual").yearlyQuota(20).build());

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Bob Staff", "bob.staff@staffsheet.com", "Password@123",
                Role.EMPLOYEE, "Engineering", LocalDate.of(2025, 6, 1), null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Employee created = employeeRepository.findByEmail("bob.staff@staffsheet.com").orElseThrow();
        assertThat(leaveBalanceRepository.countByEmployeeIdAndYear(created.getId(), Year.now().getValue()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Holiday dates are unique in the schema")
    void holidayDatesAreUnique() {
        LocalDate newYear = LocalDate.of(2026, 1, 1);
        holidayRepository.saveAndFlush(Holiday.builder().holidayDate(newYear).name("New Year").build());

        assertThat(holidayRepository.existsByHolidayDate(newYear)).isTrue();
        assertThatThrownBy(() -> holidayRepository.saveAndFlush(
                Holiday.builder().holidayDate(newYear).name("Duplicate").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CustomUserDetails principalFor(Employee target) {
        return new CustomUserDetails(
                target.getId(),
                target.getEmail(),
                target.getPasswordHash(),
                target.getRole().name(),
                target.isActive(),
                List.of(new SimpleGrantedAuthority("ROLE_" + target.getRole().name())));
    }
}
