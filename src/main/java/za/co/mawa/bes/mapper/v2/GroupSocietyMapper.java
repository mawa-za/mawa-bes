package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.dto.v2.GroupSocietyCreateRequestDto;
import za.co.mawa.bes.dto.v2.GroupSocietyResponseDto;
import za.co.mawa.bes.dto.v2.GroupSocietyUpdateRequestDto;

@Component
public class GroupSocietyMapper {

    public GroupSocietyResponseDto toResponse(GroupSocietyEntity entity) {
        if (entity == null) {
            return null;
        }

        return GroupSocietyResponseDto.builder()
                .id(entity.getId())
                .partnerId(entity.getPartnerId())
                .groupNo(entity.getGroupNo())
                .societyType(entity.getSocietyType())
                .status(entity.getStatus())
                .availableBalanceCents(entity.getAvailableBalanceCents())
                .totalPaidCents(entity.getTotalPaidCents())
                .totalClaimedCents(entity.getTotalClaimedCents())
                .lastPaymentDate(entity.getLastPaymentDate())
                .lastClaimDate(entity.getLastClaimDate())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public GroupSocietyEntity toEntity(GroupSocietyCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return GroupSocietyEntity.builder()
                .partnerId(request.getPartnerId())
                .groupNo(request.getGroupNo())
                .societyType(request.getSocietyType())
                .status(request.getStatus())
                .availableBalanceCents(request.getAvailableBalanceCents())
                .totalPaidCents(request.getTotalPaidCents())
                .totalClaimedCents(request.getTotalClaimedCents())
                .lastPaymentDate(request.getLastPaymentDate())
                .lastClaimDate(request.getLastClaimDate())
                .build();
    }

    public void updateEntity(GroupSocietyEntity entity, GroupSocietyUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPartnerId(request.getPartnerId());
        entity.setGroupNo(request.getGroupNo());
        entity.setSocietyType(request.getSocietyType());
        entity.setStatus(request.getStatus());
        entity.setAvailableBalanceCents(request.getAvailableBalanceCents());
        entity.setTotalPaidCents(request.getTotalPaidCents());
        entity.setTotalClaimedCents(request.getTotalClaimedCents());
        entity.setLastPaymentDate(request.getLastPaymentDate());
        entity.setLastClaimDate(request.getLastClaimDate());
    }
}
