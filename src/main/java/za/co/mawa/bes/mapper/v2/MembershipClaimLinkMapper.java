package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipClaimLinkEntity;
import za.co.mawa.bes.dto.v2.MembershipClaimLinkCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipClaimLinkResponseDto;
import za.co.mawa.bes.dto.v2.MembershipClaimLinkUpdateRequestDto;

@Component
public class MembershipClaimLinkMapper {

    public MembershipClaimLinkResponseDto toResponse(MembershipClaimLinkEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipClaimLinkResponseDto.builder()
                .id(entity.getId())
                .parentClaimId(entity.getParentClaimId())
                .linkedClaimId(entity.getLinkedClaimId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public MembershipClaimLinkEntity toEntity(MembershipClaimLinkCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipClaimLinkEntity.builder()
                .parentClaimId(request.getParentClaimId())
                .linkedClaimId(request.getLinkedClaimId())
                .build();
    }

    public void updateEntity(MembershipClaimLinkEntity entity, MembershipClaimLinkUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setParentClaimId(request.getParentClaimId());
        entity.setLinkedClaimId(request.getLinkedClaimId());
    }
}
