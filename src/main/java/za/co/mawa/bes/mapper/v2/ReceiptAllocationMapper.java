package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.dto.v2.ReceiptAllocationCreateRequestDto;
import za.co.mawa.bes.dto.v2.ReceiptAllocationResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptAllocationUpdateRequestDto;

@Component
public class ReceiptAllocationMapper {

    public ReceiptAllocationResponseDto toResponse(ReceiptAllocationEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReceiptAllocationResponseDto.builder()
                .id(entity.getId())
                .receiptId(entity.getReceiptId())
                .allocationType(entity.getAllocationType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .periodYYYYMM(entity.getPeriodYYYYMM())
                .membershipId(entity.getMembershipId())
                .amountCents(entity.getAmountCents())
                .status(entity.getStatus())
                .legacyPremiumPaymentId(entity.getLegacyPremiumPaymentId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public ReceiptAllocationEntity toEntity(ReceiptAllocationCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ReceiptAllocationEntity.builder()
                .receiptId(request.getReceiptId())
                .allocationType(request.getAllocationType())
                .referenceId(request.getReferenceId())
                .referenceNo(request.getReferenceNo())
                .periodYYYYMM(request.getPeriodYYYYMM())
                .membershipId(request.getMembershipId())
                .amountCents(request.getAmountCents())
                .status(request.getStatus())
                .legacyPremiumPaymentId(request.getLegacyPremiumPaymentId())
                .build();
    }

    public void updateEntity(ReceiptAllocationEntity entity, ReceiptAllocationUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setReceiptId(request.getReceiptId());
        entity.setAllocationType(request.getAllocationType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setPeriodYYYYMM(request.getPeriodYYYYMM());
        entity.setMembershipId(request.getMembershipId());
        entity.setAmountCents(request.getAmountCents());
        entity.setStatus(request.getStatus());
        entity.setLegacyPremiumPaymentId(request.getLegacyPremiumPaymentId());
    }
}
