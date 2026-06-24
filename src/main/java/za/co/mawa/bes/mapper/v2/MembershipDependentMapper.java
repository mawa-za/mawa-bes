package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.dto.v2.MembershipDependentCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipDependentResponseDto;
import za.co.mawa.bes.dto.v2.MembershipDependentUpdateRequestDto;

@Component
public class MembershipDependentMapper {

    public MembershipDependentResponseDto toResponse(MembershipDependentEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipDependentResponseDto.builder()
                .id(entity.getId())
                .membershipId(entity.getMembershipId())
                .dependentPartnerId(entity.getDependentPartnerId())
                .dependentType(entity.getDependentType())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public MembershipDependentEntity toEntity(MembershipDependentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipDependentEntity.builder()
                .membershipId(request.getMembershipId())
                .dependentPartnerId(request.getDependentPartnerId())
                .dependentType(request.getDependentType())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(MembershipDependentEntity entity, MembershipDependentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setMembershipId(request.getMembershipId());
        entity.setDependentPartnerId(request.getDependentPartnerId());
        entity.setDependentType(request.getDependentType());
        entity.setActive(request.getActive());
    }
}
