package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.SettingEntity;
import za.co.mawa.bes.dto.SettingCreateRequestDto;
import za.co.mawa.bes.dto.SettingResponseDto;
import za.co.mawa.bes.dto.SettingUpdateRequestDto;

@Component
public class SettingMapper {

    public SettingResponseDto toResponse(SettingEntity entity) {
        if (entity == null) {
            return null;
        }

        return SettingResponseDto.builder()
                .value(entity.getValue())
                .build();
    }

    public SettingEntity toEntity(SettingCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return SettingEntity.builder()
                .value(request.getValue())
                .build();
    }

    public void updateEntity(SettingEntity entity, SettingUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
    }
}
