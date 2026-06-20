package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.dto.v2.CaseEventCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseEventResponseDto;
import za.co.mawa.bes.dto.v2.CaseEventUpdateRequestDto;

@Component
public class CaseEventMapper {

    public CaseEventResponseDto toResponse(CaseEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseEventResponseDto.builder()
                .id(entity.getId())
                .location(entity.getLocation())
                .build();
    }

    public CaseEventEntity toEntity(CaseEventCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseEventEntity.builder()
                .location(request.getLocation())
                .build();
    }

    public void updateEntity(CaseEventEntity entity, CaseEventUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setLocation(request.getLocation());
    }
}
