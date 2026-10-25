package com.staffsheet.leave.mapper;

import com.staffsheet.leave.dto.LeaveBalanceResponse;
import com.staffsheet.leave.entity.LeaveBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveBalanceMapper {

    @Mapping(target = "leaveTypeId", source = "balance.leaveType.id")
    @Mapping(target = "leaveTypeName", source = "balance.leaveType.name")
    @Mapping(target = "available", source = "available")
    LeaveBalanceResponse toResponse(LeaveBalance balance, int available);
}
