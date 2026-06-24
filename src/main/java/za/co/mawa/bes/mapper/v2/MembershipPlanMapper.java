package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.dto.v2.MembershipPlanCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPlanUpdateRequestDto;

@Component
public class MembershipPlanMapper {

    public MembershipPlanResponseDto toResponse(MembershipPlanEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipPlanResponseDto.builder()
                .id(entity.getId())
                .planCode(entity.getPlanCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .premiumCents(entity.getPremiumCents())
                .currency(entity.getCurrency())
                .maxDependents(entity.getMaxDependents())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .oldId(entity.getOldId())
                .build();
    }

    public MembershipPlanEntity toEntity(MembershipPlanCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipPlanEntity.builder()
                .planCode(request.getPlanCode())
                .name(request.getName())
                .description(request.getDescription())
                .premiumCents(request.getPremiumCents())
                .currency(request.getCurrency())
                .maxDependents(request.getMaxDependents())
                .active(request.getActive())
                .oldId(request.getOldId())
                .build();
    }

    public void updateEntity(MembershipPlanEntity entity, MembershipPlanUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPlanCode(request.getPlanCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPremiumCents(request.getPremiumCents());
        entity.setCurrency(request.getCurrency());
        entity.setMaxDependents(request.getMaxDependents());
        entity.setActive(request.getActive());
        entity.setOldId(request.getOldId());
    }
}
