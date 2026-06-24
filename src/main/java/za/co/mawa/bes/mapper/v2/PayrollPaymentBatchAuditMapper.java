package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchAuditEntity;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchAuditCreateRequestDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchAuditResponseDto;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchAuditUpdateRequestDto;

@Component
public class PayrollPaymentBatchAuditMapper {

    public PayrollPaymentBatchAuditResponseDto toResponse(PayrollPaymentBatchAuditEntity entity) {
        if (entity == null) {
            return null;
        }

        return PayrollPaymentBatchAuditResponseDto.builder()
                .id(entity.getId())
                .batchId(entity.getBatchId())
                .action(entity.getAction())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public PayrollPaymentBatchAuditEntity toEntity(PayrollPaymentBatchAuditCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PayrollPaymentBatchAuditEntity.builder()
                .batchId(request.getBatchId())
                .action(request.getAction())
                .oldStatus(request.getOldStatus())
                .newStatus(request.getNewStatus())
                .message(request.getMessage())
                .build();
    }

    public void updateEntity(PayrollPaymentBatchAuditEntity entity, PayrollPaymentBatchAuditUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setBatchId(request.getBatchId());
        entity.setAction(request.getAction());
        entity.setOldStatus(request.getOldStatus());
        entity.setNewStatus(request.getNewStatus());
        entity.setMessage(request.getMessage());
    }
}
