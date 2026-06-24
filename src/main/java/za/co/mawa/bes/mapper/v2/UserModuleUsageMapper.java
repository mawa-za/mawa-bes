package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.UserModuleUsageEntity;
import za.co.mawa.bes.dto.v2.UserModuleUsageCreateRequestDto;
import za.co.mawa.bes.dto.v2.UserModuleUsageResponseDto;
import za.co.mawa.bes.dto.v2.UserModuleUsageUpdateRequestDto;

@Component
public class UserModuleUsageMapper {

    public UserModuleUsageResponseDto toResponse(UserModuleUsageEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserModuleUsageResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .moduleCode(entity.getModuleCode())
                .moduleName(entity.getModuleName())
                .modulePath(entity.getModulePath())
                .workcenterId(entity.getWorkcenterId())
                .usageCount(entity.getUsageCount())
                .firstUsedAt(entity.getFirstUsedAt())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public UserModuleUsageEntity toEntity(UserModuleUsageCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return UserModuleUsageEntity.builder()
                .userId(request.getUserId())
                .moduleCode(request.getModuleCode())
                .moduleName(request.getModuleName())
                .modulePath(request.getModulePath())
                .workcenterId(request.getWorkcenterId())
                .usageCount(request.getUsageCount())
                .firstUsedAt(request.getFirstUsedAt())
                .lastUsedAt(request.getLastUsedAt())
                .build();
    }

    public void updateEntity(UserModuleUsageEntity entity, UserModuleUsageUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setUserId(request.getUserId());
        entity.setModuleCode(request.getModuleCode());
        entity.setModuleName(request.getModuleName());
        entity.setModulePath(request.getModulePath());
        entity.setWorkcenterId(request.getWorkcenterId());
        entity.setUsageCount(request.getUsageCount());
        entity.setFirstUsedAt(request.getFirstUsedAt());
        entity.setLastUsedAt(request.getLastUsedAt());
    }
}
