package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.dto.FieldOptionCreateRequestDto;
import za.co.mawa.bes.dto.FieldOptionResponseDto;
import za.co.mawa.bes.dto.FieldOptionUpdateRequestDto;

@Component
public class FieldOptionMapper {

    public FieldOptionResponseDto toResponse(FieldOptionEntity entity) {
        if (entity == null) {
            return null;
        }

        return FieldOptionResponseDto.builder()
                .description(entity.getDescription())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public FieldOptionEntity toEntity(FieldOptionCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FieldOptionEntity.builder()
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(FieldOptionEntity entity, FieldOptionUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setDescription(request.getDescription());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
