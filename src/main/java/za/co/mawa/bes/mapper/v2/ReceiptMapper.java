package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.dto.v2.ReceiptCreateRequestDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptUpdateRequestDto;

@Component
public class ReceiptMapper {

    public ReceiptResponseDto toResponse(ReceiptEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReceiptResponseDto.builder()
                .id(entity.getId())
                .receiptNo(entity.getReceiptNo())
                .paymentBatchId(entity.getPaymentBatchId())
                .paymentBatchNo(entity.getPaymentBatchNo())
                .sourceType(entity.getSourceType())
                .receivedFromPartnerId(entity.getReceivedFromPartnerId())
                .membershipId(entity.getMembershipId())
                .receiptDate(entity.getReceiptDate())
                .paymentMethod(entity.getPaymentMethod())
                .totalAmountCents(entity.getTotalAmountCents())
                .status(entity.getStatus())
                .syncStatus(entity.getSyncStatus())
                .location(entity.getLocation())
                .employeeResponsible(entity.getEmployeeResponsible())
                .deviceId(entity.getDeviceId())
                .terminalId(entity.getTerminalId())
                .externalReceiptNo(entity.getExternalReceiptNo())
                .printed(entity.getPrinted())
                .printCount(entity.getPrintCount())
                .legacyPremiumPaymentId(entity.getLegacyPremiumPaymentId())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public ReceiptEntity toEntity(ReceiptCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ReceiptEntity.builder()
                .receiptNo(request.getReceiptNo())
                .paymentBatchId(request.getPaymentBatchId())
                .paymentBatchNo(request.getPaymentBatchNo())
                .sourceType(request.getSourceType())
                .receivedFromPartnerId(request.getReceivedFromPartnerId())
                .membershipId(request.getMembershipId())
                .receiptDate(request.getReceiptDate())
                .paymentMethod(request.getPaymentMethod())
                .totalAmountCents(request.getTotalAmountCents())
                .status(request.getStatus())
                .syncStatus(request.getSyncStatus())
                .location(request.getLocation())
                .employeeResponsible(request.getEmployeeResponsible())
                .deviceId(request.getDeviceId())
                .terminalId(request.getTerminalId())
                .externalReceiptNo(request.getExternalReceiptNo())
                .printed(request.getPrinted())
                .printCount(request.getPrintCount())
                .legacyPremiumPaymentId(request.getLegacyPremiumPaymentId())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(ReceiptEntity entity, ReceiptUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setReceiptNo(request.getReceiptNo());
        entity.setPaymentBatchId(request.getPaymentBatchId());
        entity.setPaymentBatchNo(request.getPaymentBatchNo());
        entity.setSourceType(request.getSourceType());
        entity.setReceivedFromPartnerId(request.getReceivedFromPartnerId());
        entity.setMembershipId(request.getMembershipId());
        entity.setReceiptDate(request.getReceiptDate());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setTotalAmountCents(request.getTotalAmountCents());
        entity.setStatus(request.getStatus());
        entity.setSyncStatus(request.getSyncStatus());
        entity.setLocation(request.getLocation());
        entity.setEmployeeResponsible(request.getEmployeeResponsible());
        entity.setDeviceId(request.getDeviceId());
        entity.setTerminalId(request.getTerminalId());
        entity.setExternalReceiptNo(request.getExternalReceiptNo());
        entity.setPrinted(request.getPrinted());
        entity.setPrintCount(request.getPrintCount());
        entity.setLegacyPremiumPaymentId(request.getLegacyPremiumPaymentId());
        entity.setNotes(request.getNotes());
    }
}
