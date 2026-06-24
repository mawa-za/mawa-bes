package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ModuleUsageEventEntity;
import za.co.mawa.bes.dto.v2.ModuleUsageEventCreateRequestDto;
import za.co.mawa.bes.dto.v2.ModuleUsageEventResponseDto;
import za.co.mawa.bes.dto.v2.ModuleUsageEventUpdateRequestDto;

@Component
public class ModuleUsageEventMapper {

    public ModuleUsageEventResponseDto toResponse(ModuleUsageEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return ModuleUsageEventResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .moduleCode(entity.getModuleCode())
                .moduleName(entity.getModuleName())
                .modulePath(entity.getModulePath())
                .workcenterId(entity.getWorkcenterId())
                .usedAt(entity.getUsedAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public ModuleUsageEventEntity toEntity(ModuleUsageEventCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ModuleUsageEventEntity.builder()
                .userId(request.getUserId())
                .moduleCode(request.getModuleCode())
                .moduleName(request.getModuleName())
                .modulePath(request.getModulePath())
                .workcenterId(request.getWorkcenterId())
                .usedAt(request.getUsedAt())
                .build();
    }

    public void updateEntity(ModuleUsageEventEntity entity, ModuleUsageEventUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setUserId(request.getUserId());
        entity.setModuleCode(request.getModuleCode());
        entity.setModuleName(request.getModuleName());
        entity.setModulePath(request.getModulePath());
        entity.setWorkcenterId(request.getWorkcenterId());
        entity.setUsedAt(request.getUsedAt());
    }
}
