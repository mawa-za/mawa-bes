package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.FieldOptionPKEntity;
import za.co.mawa.bes.dto.FieldOptionPKCreateRequestDto;
import za.co.mawa.bes.dto.FieldOptionPKResponseDto;
import za.co.mawa.bes.dto.FieldOptionPKUpdateRequestDto;

@Component
public class FieldOptionPKMapper {

    public FieldOptionPKResponseDto toResponse(FieldOptionPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return FieldOptionPKResponseDto.builder()
                .field(entity.getField())
                .code(entity.getCode())
                .type(entity.getType())
                .build();
    }

    public FieldOptionPKEntity toEntity(FieldOptionPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FieldOptionPKEntity.builder()
                .field(request.getField())
                .code(request.getCode())
                .type(request.getType())
                .build();
    }

    public void updateEntity(FieldOptionPKEntity entity, FieldOptionPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setField(request.getField());
        entity.setCode(request.getCode());
        entity.setType(request.getType());
    }
}
