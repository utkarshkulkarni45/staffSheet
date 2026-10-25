package com.staffsheet.leave.entity;

import com.staffsheet.common.entity.BaseEntity;
import com.staffsheet.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_leave_balances_employee_type_year",
                columnNames = {"employee_id", "leave_type_id", "balance_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "balance_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int allocated;

    @Column(nullable = false)
    @Builder.Default
    private int used = 0;

    @Column(nullable = false)
    @Builder.Default
    private int pending = 0;
}
