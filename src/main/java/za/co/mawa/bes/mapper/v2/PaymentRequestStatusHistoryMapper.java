package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.dto.v2.PaymentRequestStatusHistoryCreateRequestDto;
import za.co.mawa.bes.dto.v2.PaymentRequestStatusHistoryResponseDto;
import za.co.mawa.bes.dto.v2.PaymentRequestStatusHistoryUpdateRequestDto;

@Component
public class PaymentRequestStatusHistoryMapper {

    public PaymentRequestStatusHistoryResponseDto toResponse(PaymentRequestStatusHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentRequestStatusHistoryResponseDto.builder()
                .id(entity.getId())
                .paymentRequestId(entity.getPaymentRequestId())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .comment(entity.getComment())
                .changedAt(entity.getChangedAt())
                .changedBy(entity.getChangedBy())
                .build();
    }

    public PaymentRequestStatusHistoryEntity toEntity(PaymentRequestStatusHistoryCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PaymentRequestStatusHistoryEntity.builder()
                .paymentRequestId(request.getPaymentRequestId())
                .oldStatus(request.getOldStatus())
                .newStatus(request.getNewStatus())
                .comment(request.getComment())
                .changedAt(request.getChangedAt())
                .changedBy(request.getChangedBy())
                .build();
    }

    public void updateEntity(PaymentRequestStatusHistoryEntity entity, PaymentRequestStatusHistoryUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPaymentRequestId(request.getPaymentRequestId());
        entity.setOldStatus(request.getOldStatus());
        entity.setNewStatus(request.getNewStatus());
        entity.setComment(request.getComment());
        entity.setChangedAt(request.getChangedAt());
        entity.setChangedBy(request.getChangedBy());
    }
}
