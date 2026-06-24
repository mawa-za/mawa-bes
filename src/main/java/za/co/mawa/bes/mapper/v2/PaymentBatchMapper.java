package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.dto.v2.PaymentBatchCreateRequestDto;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.PaymentBatchUpdateRequestDto;

@Component
public class PaymentBatchMapper {

    public PaymentBatchResponseDto toResponse(PaymentBatchEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentBatchResponseDto.builder()
                .id(entity.getId())
                .paymentBatchNo(entity.getPaymentBatchNo())
                .sourceType(entity.getSourceType())
                .receivedFromPartnerId(entity.getReceivedFromPartnerId())
                .membershipId(entity.getMembershipId())
                .paymentMethod(entity.getPaymentMethod())
                .totalAmountCents(entity.getTotalAmountCents())
                .paymentDate(entity.getPaymentDate())
                .location(entity.getLocation())
                .employeeResponsible(entity.getEmployeeResponsible())
                .deviceId(entity.getDeviceId())
                .terminalId(entity.getTerminalId())
                .localPaymentBatchId(entity.getLocalPaymentBatchId())
                .status(entity.getStatus())
                .syncStatus(entity.getSyncStatus())
                .notes(entity.getNotes())
                .legacyPremiumPaymentId(entity.getLegacyPremiumPaymentId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public PaymentBatchEntity toEntity(PaymentBatchCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PaymentBatchEntity.builder()
                .paymentBatchNo(request.getPaymentBatchNo())
                .sourceType(request.getSourceType())
                .receivedFromPartnerId(request.getReceivedFromPartnerId())
                .membershipId(request.getMembershipId())
                .paymentMethod(request.getPaymentMethod())
                .totalAmountCents(request.getTotalAmountCents())
                .paymentDate(request.getPaymentDate())
                .location(request.getLocation())
                .employeeResponsible(request.getEmployeeResponsible())
                .deviceId(request.getDeviceId())
                .terminalId(request.getTerminalId())
                .localPaymentBatchId(request.getLocalPaymentBatchId())
                .status(request.getStatus())
                .syncStatus(request.getSyncStatus())
                .notes(request.getNotes())
                .legacyPremiumPaymentId(request.getLegacyPremiumPaymentId())
                .build();
    }

    public void updateEntity(PaymentBatchEntity entity, PaymentBatchUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPaymentBatchNo(request.getPaymentBatchNo());
        entity.setSourceType(request.getSourceType());
        entity.setReceivedFromPartnerId(request.getReceivedFromPartnerId());
        entity.setMembershipId(request.getMembershipId());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setTotalAmountCents(request.getTotalAmountCents());
        entity.setPaymentDate(request.getPaymentDate());
        entity.setLocation(request.getLocation());
        entity.setEmployeeResponsible(request.getEmployeeResponsible());
        entity.setDeviceId(request.getDeviceId());
        entity.setTerminalId(request.getTerminalId());
        entity.setLocalPaymentBatchId(request.getLocalPaymentBatchId());
        entity.setStatus(request.getStatus());
        entity.setSyncStatus(request.getSyncStatus());
        entity.setNotes(request.getNotes());
        entity.setLegacyPremiumPaymentId(request.getLegacyPremiumPaymentId());
    }
}
