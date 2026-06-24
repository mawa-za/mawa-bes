package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralMembershipCoverEntity;
import za.co.mawa.bes.dto.v2.FuneralMembershipCoverCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralMembershipCoverResponseDto;
import za.co.mawa.bes.dto.v2.FuneralMembershipCoverUpdateRequestDto;

@Component
public class FuneralMembershipCoverMapper {

    public FuneralMembershipCoverResponseDto toResponse(FuneralMembershipCoverEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralMembershipCoverResponseDto.builder()
                .id(entity.getId())
                .identityNumber(entity.getIdentityNumber())
                .membershipId(entity.getMembershipId())
                .membershipNumber(entity.getMembershipNumber())
                .burialSocietyPartnerId(entity.getBurialSocietyPartnerId())
                .burialSocietyName(entity.getBurialSocietyName())
                .coverAmountCents(entity.getCoverAmountCents())
                .status(entity.getStatus())
                .build();
    }

    public FuneralMembershipCoverEntity toEntity(FuneralMembershipCoverCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralMembershipCoverEntity.builder()
                .identityNumber(request.getIdentityNumber())
                .membershipId(request.getMembershipId())
                .membershipNumber(request.getMembershipNumber())
                .burialSocietyPartnerId(request.getBurialSocietyPartnerId())
                .burialSocietyName(request.getBurialSocietyName())
                .coverAmountCents(request.getCoverAmountCents())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(FuneralMembershipCoverEntity entity, FuneralMembershipCoverUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setIdentityNumber(request.getIdentityNumber());
        entity.setMembershipId(request.getMembershipId());
        entity.setMembershipNumber(request.getMembershipNumber());
        entity.setBurialSocietyPartnerId(request.getBurialSocietyPartnerId());
        entity.setBurialSocietyName(request.getBurialSocietyName());
        entity.setCoverAmountCents(request.getCoverAmountCents());
        entity.setStatus(request.getStatus());
    }
}
