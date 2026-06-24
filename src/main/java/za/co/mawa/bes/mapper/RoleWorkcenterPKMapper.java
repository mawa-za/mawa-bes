package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.dto.RoleWorkcenterPKCreateRequestDto;
import za.co.mawa.bes.dto.RoleWorkcenterPKResponseDto;
import za.co.mawa.bes.dto.RoleWorkcenterPKUpdateRequestDto;

@Component
public class RoleWorkcenterPKMapper {

    public RoleWorkcenterPKResponseDto toResponse(RoleWorkcenterPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return RoleWorkcenterPKResponseDto.builder()
                .role(entity.getRole())
                .workcenter(entity.getWorkcenter())
                .build();
    }

    public RoleWorkcenterPKEntity toEntity(RoleWorkcenterPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return RoleWorkcenterPKEntity.builder()
                .role(request.getRole())
                .workcenter(request.getWorkcenter())
                .build();
    }

    public void updateEntity(RoleWorkcenterPKEntity entity, RoleWorkcenterPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setRole(request.getRole());
        entity.setWorkcenter(request.getWorkcenter());
    }
}
