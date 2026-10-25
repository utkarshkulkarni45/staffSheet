package com.staffsheet.leave.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.leave.dto.LeaveTypeRequest;
import com.staffsheet.leave.dto.LeaveTypeResponse;
import com.staffsheet.leave.entity.LeaveType;
import com.staffsheet.leave.mapper.LeaveTypeMapper;
import com.staffsheet.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeMapper leaveTypeMapper;

    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        String name = request.name().trim();
        if (leaveTypeRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Leave type '" + name + "' already exists");
        }

        LeaveType leaveType = leaveTypeMapper.toEntity(request);
        leaveType.setName(name);

        return leaveTypeMapper.toResponse(leaveTypeRepository.save(leaveType));
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAllByOrderByNameAsc().stream()
                .map(leaveTypeMapper::toResponse)
                .toList();
    }
}
