package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchCreateRequestDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchUpdateRequestDto;

@Component
public class PayrollPaymentBatchMapper {

    public PayrollPaymentBatchResponseDto toResponse(PayrollPaymentBatchEntity entity) {
        if (entity == null) {
            return null;
        }

        return PayrollPaymentBatchResponseDto.builder()
                .id(entity.getId())
                .batchNo(entity.getBatchNo())
                .description(entity.getDescription())
                .payPeriod(entity.getPayPeriod())
                .paymentDate(entity.getPaymentDate())
                .sourceBatchId(entity.getSourceBatchId())
                .status(entity.getStatus())
                .totalEmployees(entity.getTotalEmployees())
                .totalAmountCents(entity.getTotalAmountCents())
                .eftFileGenerated(entity.getEftFileGenerated())
                .eftFileName(entity.getEftFileName())
                .eftFileGeneratedAt(entity.getEftFileGeneratedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public PayrollPaymentBatchEntity toEntity(PayrollPaymentBatchCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PayrollPaymentBatchEntity.builder()
                .batchNo(request.getBatchNo())
                .description(request.getDescription())
                .payPeriod(request.getPayPeriod())
                .paymentDate(request.getPaymentDate())
                .sourceBatchId(request.getSourceBatchId())
                .status(request.getStatus())
                .totalEmployees(request.getTotalEmployees())
                .totalAmountCents(request.getTotalAmountCents())
                .eftFileGenerated(request.getEftFileGenerated())
                .eftFileName(request.getEftFileName())
                .eftFileGeneratedAt(request.getEftFileGeneratedAt())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(PayrollPaymentBatchEntity entity, PayrollPaymentBatchUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setBatchNo(request.getBatchNo());
        entity.setDescription(request.getDescription());
        entity.setPayPeriod(request.getPayPeriod());
        entity.setPaymentDate(request.getPaymentDate());
        entity.setSourceBatchId(request.getSourceBatchId());
        entity.setStatus(request.getStatus());
        entity.setTotalEmployees(request.getTotalEmployees());
        entity.setTotalAmountCents(request.getTotalAmountCents());
        entity.setEftFileGenerated(request.getEftFileGenerated());
        entity.setEftFileName(request.getEftFileName());
        entity.setEftFileGeneratedAt(request.getEftFileGeneratedAt());
        entity.setNotes(request.getNotes());
    }
}
