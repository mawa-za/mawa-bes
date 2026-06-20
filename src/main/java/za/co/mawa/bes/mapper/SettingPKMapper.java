package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.SettingPKEntity;
import za.co.mawa.bes.dto.SettingPKCreateRequestDto;
import za.co.mawa.bes.dto.SettingPKResponseDto;
import za.co.mawa.bes.dto.SettingPKUpdateRequestDto;

@Component
public class SettingPKMapper {

    public SettingPKResponseDto toResponse(SettingPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return SettingPKResponseDto.builder()
                .setting(entity.getSetting())
                .attribute(entity.getAttribute())
                .build();
    }

    public SettingPKEntity toEntity(SettingPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return SettingPKEntity.builder()
                .setting(request.getSetting())
                .attribute(request.getAttribute())
                .build();
    }

    public void updateEntity(SettingPKEntity entity, SettingPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setSetting(request.getSetting());
        entity.setAttribute(request.getAttribute());
    }
}
