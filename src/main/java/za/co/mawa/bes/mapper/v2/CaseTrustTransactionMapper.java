package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseTrustTransactionEntity;
import za.co.mawa.bes.dto.v2.CaseTrustTransactionCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseTrustTransactionResponseDto;
import za.co.mawa.bes.dto.v2.CaseTrustTransactionUpdateRequestDto;

@Component
public class CaseTrustTransactionMapper {

    public CaseTrustTransactionResponseDto toResponse(CaseTrustTransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseTrustTransactionResponseDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .clientPartnerId(entity.getClientPartnerId())
                .transactionNo(entity.getTransactionNo())
                .transactionType(entity.getTransactionType())
                .direction(entity.getDirection())
                .amountCents(entity.getAmountCents())
                .balanceAfterCents(entity.getBalanceAfterCents())
                .paymentMethod(entity.getPaymentMethod())
                .referenceNo(entity.getReferenceNo())
                .bankReference(entity.getBankReference())
                .payeeName(entity.getPayeeName())
                .description(entity.getDescription())
                .relatedInvoiceId(entity.getRelatedInvoiceId())
                .relatedReceiptId(entity.getRelatedReceiptId())
                .relatedTransactionId(entity.getRelatedTransactionId())
                .transactionDate(entity.getTransactionDate())
                .reversed(entity.getReversed())
                .reversedAt(entity.getReversedAt())
                .reversedBy(entity.getReversedBy())
                .reversalReason(entity.getReversalReason())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public CaseTrustTransactionEntity toEntity(CaseTrustTransactionCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseTrustTransactionEntity.builder()
                .caseId(request.getCaseId())
                .clientPartnerId(request.getClientPartnerId())
                .transactionNo(request.getTransactionNo())
                .transactionType(request.getTransactionType())
                .direction(request.getDirection())
                .amountCents(request.getAmountCents())
                .balanceAfterCents(request.getBalanceAfterCents())
                .paymentMethod(request.getPaymentMethod())
                .referenceNo(request.getReferenceNo())
                .bankReference(request.getBankReference())
                .payeeName(request.getPayeeName())
                .description(request.getDescription())
                .relatedInvoiceId(request.getRelatedInvoiceId())
                .relatedReceiptId(request.getRelatedReceiptId())
                .relatedTransactionId(request.getRelatedTransactionId())
                .transactionDate(request.getTransactionDate())
                .reversed(request.getReversed())
                .reversedAt(request.getReversedAt())
                .reversedBy(request.getReversedBy())
                .reversalReason(request.getReversalReason())
                .build();
    }

    public void updateEntity(CaseTrustTransactionEntity entity, CaseTrustTransactionUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCaseId(request.getCaseId());
        entity.setClientPartnerId(request.getClientPartnerId());
        entity.setTransactionNo(request.getTransactionNo());
        entity.setTransactionType(request.getTransactionType());
        entity.setDirection(request.getDirection());
        entity.setAmountCents(request.getAmountCents());
        entity.setBalanceAfterCents(request.getBalanceAfterCents());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setBankReference(request.getBankReference());
        entity.setPayeeName(request.getPayeeName());
        entity.setDescription(request.getDescription());
        entity.setRelatedInvoiceId(request.getRelatedInvoiceId());
        entity.setRelatedReceiptId(request.getRelatedReceiptId());
        entity.setRelatedTransactionId(request.getRelatedTransactionId());
        entity.setTransactionDate(request.getTransactionDate());
        entity.setReversed(request.getReversed());
        entity.setReversedAt(request.getReversedAt());
        entity.setReversedBy(request.getReversedBy());
        entity.setReversalReason(request.getReversalReason());
    }
}
