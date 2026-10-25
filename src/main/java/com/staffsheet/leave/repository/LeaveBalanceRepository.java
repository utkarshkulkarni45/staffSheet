package com.staffsheet.leave.repository;

import com.staffsheet.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    @EntityGraph(attributePaths = "leaveType")
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);

    long countByEmployeeIdAndYear(Long employeeId, int year);
}
