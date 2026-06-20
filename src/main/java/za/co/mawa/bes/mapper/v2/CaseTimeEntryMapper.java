package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.dto.v2.CaseTimeEntryCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseTimeEntryResponseDto;
import za.co.mawa.bes.dto.v2.CaseTimeEntryUpdateRequestDto;

@Component
public class CaseTimeEntryMapper {

    public CaseTimeEntryResponseDto toResponse(CaseTimeEntryEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseTimeEntryResponseDto.builder()
                .id(entity.getId())
                .build();
    }

    public CaseTimeEntryEntity toEntity(CaseTimeEntryCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseTimeEntryEntity.builder()

                .build();
    }

    public void updateEntity(CaseTimeEntryEntity entity, CaseTimeEntryUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
    }
}
