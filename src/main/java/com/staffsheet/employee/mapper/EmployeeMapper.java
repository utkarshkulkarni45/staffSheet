package com.staffsheet.employee.mapper;

import com.staffsheet.employee.dto.CreateEmployeeRequest;
import com.staffsheet.employee.dto.EmployeeResponse;
import com.staffsheet.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "reports", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", source = "manager.name")
    EmployeeResponse toResponse(Employee employee);
}


