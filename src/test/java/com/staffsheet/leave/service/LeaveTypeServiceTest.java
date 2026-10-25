package com.staffsheet.leave.service;

import com.staffsheet.common.exception.BadRequestException;
import com.staffsheet.leave.dto.LeaveTypeRequest;
import com.staffsheet.leave.dto.LeaveTypeResponse;
import com.staffsheet.leave.entity.LeaveType;
import com.staffsheet.leave.mapper.LeaveTypeMapper;
import com.staffsheet.leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveTypeMapper leaveTypeMapper;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    @Test
    @DisplayName("Should trim the name and persist a new leave type")
    void createLeaveType_Success() {
        LeaveTypeRequest request = new LeaveTypeRequest("  Annual  ", 20, true);
        LeaveType entity = LeaveType.builder().name("Annual").yearlyQuota(20).carryForward(true).build();
        entity.setId(1L);
        LeaveTypeResponse expected = new LeaveTypeResponse(1L, "Annual", 20, true, 0L);

        when(leaveTypeRepository.existsByNameIgnoreCase("Annual")).thenReturn(false);
        when(leaveTypeMapper.toEntity(request)).thenReturn(entity);
        when(leaveTypeRepository.save(entity)).thenReturn(entity);
        when(leaveTypeMapper.toResponse(entity)).thenReturn(expected);

        LeaveTypeResponse response = leaveTypeService.createLeaveType(request);

        assertThat(response).isEqualTo(expected);
        assertThat(entity.getName()).isEqualTo("Annual");
        verify(leaveTypeRepository).save(entity);
    }

    @Test
    @DisplayName("Should reject a duplicate leave type name regardless of case")
    void createLeaveType_DuplicateName_ThrowsBadRequest() {
        LeaveTypeRequest request = new LeaveTypeRequest("annual", 20, false);

        when(leaveTypeRepository.existsByNameIgnoreCase("annual")).thenReturn(true);

        assertThatThrownBy(() -> leaveTypeService.createLeaveType(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(leaveTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all leave types sorted by the repository query")
    void getAllLeaveTypes_ReturnsMappedList() {
        LeaveType annual = LeaveType.builder().name("Annual").yearlyQuota(20).build();
        annual.setId(1L);
        LeaveTypeResponse expected = new LeaveTypeResponse(1L, "Annual", 20, false, 0L);

        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(annual));
        when(leaveTypeMapper.toResponse(annual)).thenReturn(expected);

        assertThat(leaveTypeService.getAllLeaveTypes()).containsExactly(expected);
    }
}
