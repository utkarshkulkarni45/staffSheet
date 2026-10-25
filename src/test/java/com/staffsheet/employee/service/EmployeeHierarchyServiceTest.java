package com.staffsheet.employee.service;

import com.staffsheet.employee.dto.HierarchyFlatResponse;
import com.staffsheet.employee.dto.HierarchyNodeResponse;
import com.staffsheet.employee.entity.Role;
import com.staffsheet.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeHierarchyServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("Should retrieve upward management chain using native CTE results")
    void getUpwardChain_Success() {
        when(employeeRepository.existsById(10L)).thenReturn(true);

        List<Object[]> mockRows = List.of(
                new Object[]{10L, "Alice Developer", "alice@staffsheet.com", "EMPLOYEE", "Engineering", 5L, 0},
                new Object[]{5L, "Bob Manager", "bob@staffsheet.com", "MANAGER", "Engineering", 1L, 1},
                new Object[]{1L, "Charlie VP", "charlie@staffsheet.com", "ADMIN", "Executive", null, 2}
        );

        when(employeeRepository.findUpwardChainNative(10L)).thenReturn(mockRows);

        List<HierarchyFlatResponse> chain = employeeService.getUpwardChain(10L);

        assertThat(chain).hasSize(3);
        assertThat(chain.get(0).name()).isEqualTo("Alice Developer");
        assertThat(chain.get(0).depth()).isEqualTo(0);
        assertThat(chain.get(1).name()).isEqualTo("Bob Manager");
        assertThat(chain.get(1).depth()).isEqualTo(1);
        assertThat(chain.get(2).name()).isEqualTo("Charlie VP");
        assertThat(chain.get(2).depth()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should build recursive hierarchy subtree tree rooted at given manager")
    void getReportingSubtree_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        List<Object[]> mockRows = List.of(
                new Object[]{1L, "Charlie VP", "charlie@staffsheet.com", "ADMIN", "Executive", null, 0},
                new Object[]{5L, "Bob Manager", "bob@staffsheet.com", "MANAGER", "Engineering", 1L, 1},
                new Object[]{10L, "Alice Developer", "alice@staffsheet.com", "EMPLOYEE", "Engineering", 5L, 2}
        );

        when(employeeRepository.findDownwardSubtreeNative(1L)).thenReturn(mockRows);

        HierarchyNodeResponse tree = employeeService.getReportingSubtree(1L);

        assertThat(tree).isNotNull();
        assertThat(tree.name()).isEqualTo("Charlie VP");
        assertThat(tree.level()).isEqualTo(0);
        assertThat(tree.directReports()).hasSize(1);

        HierarchyNodeResponse managerNode = tree.directReports().get(0);
        assertThat(managerNode.name()).isEqualTo("Bob Manager");
        assertThat(managerNode.level()).isEqualTo(1);
        assertThat(managerNode.directReports()).hasSize(1);

        HierarchyNodeResponse devNode = managerNode.directReports().get(0);
        assertThat(devNode.name()).isEqualTo("Alice Developer");
        assertThat(devNode.level()).isEqualTo(2);
        assertThat(devNode.directReports()).isEmpty();
    }
}

