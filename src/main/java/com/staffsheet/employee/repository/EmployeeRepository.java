package com.staffsheet.employee.repository;

import com.staffsheet.employee.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"manager"})
    Optional<Employee> findWithManagerById(Long id);

    @EntityGraph(attributePaths = {"manager"})
    List<Employee> findByManagerId(Long managerId);

    /**
     * Upward management chain query using Recursive CTE.
     * Starts from given employee and climbs up to top-level manager.
     */
    @Query(value = """
            WITH RECURSIVE reporting_chain AS (
                SELECT id, name, email, role, department, manager_id, 0 AS depth
                FROM employees
                WHERE id = :employeeId
                UNION ALL
                SELECT e.id, e.name, e.email, e.role, e.department, e.manager_id, rc.depth + 1
                FROM employees e
                INNER JOIN reporting_chain rc ON e.id = rc.manager_id
            )
            SELECT id, name, email, role, department, manager_id, depth
            FROM reporting_chain
            ORDER BY depth ASC
            """, nativeQuery = true)
    List<Object[]> findUpwardChainNative(@Param("employeeId") Long employeeId);

    /**
     * Downward management subtree query using Recursive CTE.
     * Starts from given manager and traverses all direct and indirect reports downwards.
     */
    @Query(value = """
            WITH RECURSIVE reporting_subtree AS (
                SELECT id, name, email, role, department, manager_id, 0 AS depth
                FROM employees
                WHERE id = :managerId
                UNION ALL
                SELECT e.id, e.name, e.email, e.role, e.department, e.manager_id, rs.depth + 1
                FROM employees e
                INNER JOIN reporting_subtree rs ON e.manager_id = rs.id
            )
            SELECT id, name, email, role, department, manager_id, depth
            FROM reporting_subtree
            ORDER BY depth ASC, name ASC
            """, nativeQuery = true)
    List<Object[]> findDownwardSubtreeNative(@Param("managerId") Long managerId);
}


