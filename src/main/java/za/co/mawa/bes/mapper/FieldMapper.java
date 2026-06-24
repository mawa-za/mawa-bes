package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.FieldEntity;
import za.co.mawa.bes.dto.FieldCreateRequestDto;
import za.co.mawa.bes.dto.FieldResponseDto;
import za.co.mawa.bes.dto.FieldUpdateRequestDto;

@Component
public class FieldMapper {

    public FieldResponseDto toResponse(FieldEntity entity) {
        if (entity == null) {
            return null;
        }

        return FieldResponseDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public FieldEntity toEntity(FieldCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FieldEntity.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(FieldEntity entity, FieldUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
