package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.dto.EmploymentCreateRequestDto;
import za.co.mawa.bes.dto.EmploymentResponseDto;
import za.co.mawa.bes.dto.EmploymentUpdateRequestDto;

@Component
public class EmploymentMapper {

    public EmploymentResponseDto toResponse(EmploymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return EmploymentResponseDto.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .employeeNumber(entity.getEmployeeNumber())
                .branch(entity.getBranch())
                .department(entity.getDepartment())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .position(entity.getPosition())
                .status(entity.getStatus())
                .type(entity.getType())
                .build();
    }

    public EmploymentEntity toEntity(EmploymentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return EmploymentEntity.builder()
                .partnerId(request.getPartnerId())
                .employeeNumber(request.getEmployeeNumber())
                .branch(request.getBranch())
                .department(request.getDepartment())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .position(request.getPosition())
                .status(request.getStatus())
                .type(request.getType())
                .build();
    }

    public void updateEntity(EmploymentEntity entity, EmploymentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPartnerId(request.getPartnerId());
        entity.setEmployeeNumber(request.getEmployeeNumber());
        entity.setBranch(request.getBranch());
        entity.setDepartment(request.getDepartment());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setPosition(request.getPosition());
        entity.setStatus(request.getStatus());
        entity.setType(request.getType());
    }
}
