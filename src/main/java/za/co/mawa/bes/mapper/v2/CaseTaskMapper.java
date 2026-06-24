package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.dto.v2.CaseTaskCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseTaskResponseDto;
import za.co.mawa.bes.dto.v2.CaseTaskUpdateRequestDto;

@Component
public class CaseTaskMapper {

    public CaseTaskResponseDto toResponse(CaseTaskEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseTaskResponseDto.builder()
                .id(entity.getId())
                .build();
    }

    public CaseTaskEntity toEntity(CaseTaskCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseTaskEntity.builder()

                .build();
    }

    public void updateEntity(CaseTaskEntity entity, CaseTaskUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
    }
}
