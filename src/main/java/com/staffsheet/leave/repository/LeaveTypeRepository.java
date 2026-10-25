package com.staffsheet.leave.repository;

import com.staffsheet.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<LeaveType> findByNameIgnoreCase(String name);
}
