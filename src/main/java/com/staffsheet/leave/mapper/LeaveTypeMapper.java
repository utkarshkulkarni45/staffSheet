package com.staffsheet.leave.mapper;

import com.staffsheet.leave.dto.LeaveTypeRequest;
import com.staffsheet.leave.dto.LeaveTypeResponse;
import com.staffsheet.leave.entity.LeaveType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeaveTypeMapper {

    LeaveType toEntity(LeaveTypeRequest request);

    LeaveTypeResponse toResponse(LeaveType leaveType);
}
