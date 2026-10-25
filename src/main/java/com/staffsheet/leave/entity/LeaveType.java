package com.staffsheet.leave.entity;

import com.staffsheet.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types",
        uniqueConstraints = @UniqueConstraint(name = "uq_leave_types_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "yearly_quota", nullable = false)
    private int yearlyQuota;

    @Column(name = "carry_forward", nullable = false)
    @Builder.Default
    private boolean carryForward = false;
}
