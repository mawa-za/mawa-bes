package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseNoteEntity;
import za.co.mawa.bes.dto.v2.CaseNoteCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseNoteResponseDto;
import za.co.mawa.bes.dto.v2.CaseNoteUpdateRequestDto;

@Component
public class CaseNoteMapper {

    public CaseNoteResponseDto toResponse(CaseNoteEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseNoteResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .build();
    }

    public CaseNoteEntity toEntity(CaseNoteCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseNoteEntity.builder()
                .title(request.getTitle())
                .build();
    }

    public void updateEntity(CaseNoteEntity entity, CaseNoteUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setTitle(request.getTitle());
    }
}
