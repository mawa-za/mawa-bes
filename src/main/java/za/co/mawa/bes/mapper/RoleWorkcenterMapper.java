package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.RoleWorkcenterEntity;
import za.co.mawa.bes.dto.RoleWorkcenterCreateRequestDto;
import za.co.mawa.bes.dto.RoleWorkcenterResponseDto;
import za.co.mawa.bes.dto.RoleWorkcenterUpdateRequestDto;

@Component
public class RoleWorkcenterMapper {

    public RoleWorkcenterResponseDto toResponse(RoleWorkcenterEntity entity) {
        if (entity == null) {
            return null;
        }

        return RoleWorkcenterResponseDto.builder()
                .position(entity.getPosition())
                .build();
    }

    public RoleWorkcenterEntity toEntity(RoleWorkcenterCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return RoleWorkcenterEntity.builder()
                .position(request.getPosition())
                .build();
    }

    public void updateEntity(RoleWorkcenterEntity entity, RoleWorkcenterUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPosition(request.getPosition());
    }
}
