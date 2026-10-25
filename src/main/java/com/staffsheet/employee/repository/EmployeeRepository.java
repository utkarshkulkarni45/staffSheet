package com.staffsheet.employee.repository;

import com.staffsheet.employee.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"manager"})
    Optional<Employee> findWithManagerById(Long id);
}

