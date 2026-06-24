package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.dto.v2.MembershipCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipResponseDto;
import za.co.mawa.bes.dto.v2.MembershipUpdateRequestDto;

@Component
public class MembershipMapper {

    public MembershipResponseDto toResponse(MembershipEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipResponseDto.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .membershipNo(entity.getMembershipNo())
                .planId(entity.getPlanId())
                .premiumCents(entity.getPremiumCents())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .paidUpToPeriod(entity.getPaidUpToPeriod())
                .joinDate(entity.getJoinDate())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .oldId(entity.getOldId())
                .build();
    }

    public MembershipEntity toEntity(MembershipCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipEntity.builder()
                .memberId(request.getMemberId())
                .membershipNo(request.getMembershipNo())
                .planId(request.getPlanId())
                .premiumCents(request.getPremiumCents())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .paidUpToPeriod(request.getPaidUpToPeriod())
                .joinDate(request.getJoinDate())
                .oldId(request.getOldId())
                .build();
    }

    public void updateEntity(MembershipEntity entity, MembershipUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setMemberId(request.getMemberId());
        entity.setMembershipNo(request.getMembershipNo());
        entity.setPlanId(request.getPlanId());
        entity.setPremiumCents(request.getPremiumCents());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setStatus(request.getStatus());
        entity.setPaidUpToPeriod(request.getPaidUpToPeriod());
        entity.setJoinDate(request.getJoinDate());
        entity.setOldId(request.getOldId());
    }
}
