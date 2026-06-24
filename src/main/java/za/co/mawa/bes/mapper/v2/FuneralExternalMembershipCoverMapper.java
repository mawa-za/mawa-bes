package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralExternalMembershipCoverEntity;
import za.co.mawa.bes.dto.v2.FuneralExternalMembershipCoverCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralExternalMembershipCoverResponseDto;
import za.co.mawa.bes.dto.v2.FuneralExternalMembershipCoverUpdateRequestDto;

@Component
public class FuneralExternalMembershipCoverMapper {

    public FuneralExternalMembershipCoverResponseDto toResponse(FuneralExternalMembershipCoverEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralExternalMembershipCoverResponseDto.builder()
                .id(entity.getId())
                .identityNumber(entity.getIdentityNumber())
                .sourceTenantId(entity.getSourceTenantId())
                .sourceTenantName(entity.getSourceTenantName())
                .sourceMembershipId(entity.getSourceMembershipId())
                .sourceMembershipNo(entity.getSourceMembershipNo())
                .sourceReference(entity.getSourceReference())
                .burialSocietyPartnerId(entity.getBurialSocietyPartnerId())
                .burialSocietyName(entity.getBurialSocietyName())
                .coverAmountCents(entity.getCoverAmountCents())
                .status(entity.getStatus())
                .lastVerifiedAt(entity.getLastVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FuneralExternalMembershipCoverEntity toEntity(FuneralExternalMembershipCoverCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralExternalMembershipCoverEntity.builder()
                .identityNumber(request.getIdentityNumber())
                .sourceTenantId(request.getSourceTenantId())
                .sourceTenantName(request.getSourceTenantName())
                .sourceMembershipId(request.getSourceMembershipId())
                .sourceMembershipNo(request.getSourceMembershipNo())
                .sourceReference(request.getSourceReference())
                .burialSocietyPartnerId(request.getBurialSocietyPartnerId())
                .burialSocietyName(request.getBurialSocietyName())
                .coverAmountCents(request.getCoverAmountCents())
                .status(request.getStatus())
                .lastVerifiedAt(request.getLastVerifiedAt())
                .build();
    }

    public void updateEntity(FuneralExternalMembershipCoverEntity entity, FuneralExternalMembershipCoverUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setIdentityNumber(request.getIdentityNumber());
        entity.setSourceTenantId(request.getSourceTenantId());
        entity.setSourceTenantName(request.getSourceTenantName());
        entity.setSourceMembershipId(request.getSourceMembershipId());
        entity.setSourceMembershipNo(request.getSourceMembershipNo());
        entity.setSourceReference(request.getSourceReference());
        entity.setBurialSocietyPartnerId(request.getBurialSocietyPartnerId());
        entity.setBurialSocietyName(request.getBurialSocietyName());
        entity.setCoverAmountCents(request.getCoverAmountCents());
        entity.setStatus(request.getStatus());
        entity.setLastVerifiedAt(request.getLastVerifiedAt());
    }
}
