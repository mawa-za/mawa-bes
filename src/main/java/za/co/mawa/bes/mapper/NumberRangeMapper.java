package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.dto.NumberRangeCreateRequestDto;
import za.co.mawa.bes.dto.NumberRangeResponseDto;
import za.co.mawa.bes.dto.NumberRangeUpdateRequestDto;

@Component
public class NumberRangeMapper {

    public NumberRangeResponseDto toResponse(NumberRangeEntity entity) {
        if (entity == null) {
            return null;
        }

        return NumberRangeResponseDto.builder()
                .id(entity.getId())
                .object(entity.getObject())
                .prefix(entity.getPrefix())
                .start(entity.getStart())
                .current(entity.getCurrent())
                .end(entity.getEnd())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public NumberRangeEntity toEntity(NumberRangeCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return NumberRangeEntity.builder()
                .object(request.getObject())
                .prefix(request.getPrefix())
                .start(request.getStart())
                .current(request.getCurrent())
                .end(request.getEnd())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(NumberRangeEntity entity, NumberRangeUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setObject(request.getObject());
        entity.setPrefix(request.getPrefix());
        entity.setStart(request.getStart());
        entity.setCurrent(request.getCurrent());
        entity.setEnd(request.getEnd());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
