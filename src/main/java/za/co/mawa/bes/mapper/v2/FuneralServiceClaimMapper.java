package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralServiceClaimEntity;
import za.co.mawa.bes.dto.v2.FuneralServiceClaimCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralServiceClaimResponseDto;
import za.co.mawa.bes.dto.v2.FuneralServiceClaimUpdateRequestDto;

@Component
public class FuneralServiceClaimMapper {

    public FuneralServiceClaimResponseDto toResponse(FuneralServiceClaimEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralServiceClaimResponseDto.builder()
                .id(entity.getId())
                .funeralServiceId(entity.getFuneralServiceId())
                .membershipClaimId(entity.getMembershipClaimId())
                .coverSource(entity.getCoverSource())
                .sourceTenantId(entity.getSourceTenantId())
                .sourceTenantName(entity.getSourceTenantName())
                .sourceMembershipId(entity.getSourceMembershipId())
                .sourceReference(entity.getSourceReference())
                .burialSocietyPartnerId(entity.getBurialSocietyPartnerId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public FuneralServiceClaimEntity toEntity(FuneralServiceClaimCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralServiceClaimEntity.builder()
                .funeralServiceId(request.getFuneralServiceId())
                .membershipClaimId(request.getMembershipClaimId())
                .coverSource(request.getCoverSource())
                .sourceTenantId(request.getSourceTenantId())
                .sourceTenantName(request.getSourceTenantName())
                .sourceMembershipId(request.getSourceMembershipId())
                .sourceReference(request.getSourceReference())
                .burialSocietyPartnerId(request.getBurialSocietyPartnerId())
                .build();
    }

    public void updateEntity(FuneralServiceClaimEntity entity, FuneralServiceClaimUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setFuneralServiceId(request.getFuneralServiceId());
        entity.setMembershipClaimId(request.getMembershipClaimId());
        entity.setCoverSource(request.getCoverSource());
        entity.setSourceTenantId(request.getSourceTenantId());
        entity.setSourceTenantName(request.getSourceTenantName());
        entity.setSourceMembershipId(request.getSourceMembershipId());
        entity.setSourceReference(request.getSourceReference());
        entity.setBurialSocietyPartnerId(request.getBurialSocietyPartnerId());
    }
}
