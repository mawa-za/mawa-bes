package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.dto.v2.PaymentRequestCreateRequestDto;
import za.co.mawa.bes.dto.v2.PaymentRequestResponseDto;
import za.co.mawa.bes.dto.v2.PaymentRequestUpdateRequestDto;

@Component
public class PaymentRequestMapper {

    public PaymentRequestResponseDto toResponse(PaymentRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentRequestResponseDto.builder()
                .id(entity.getId())
                .requestNo(entity.getRequestNo())
                .requestType(entity.getRequestType())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .payeePartnerId(entity.getPayeePartnerId())
                .payeeName(entity.getPayeeName())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .bankName(entity.getBankName())
                .accountHolder(entity.getAccountHolder())
                .accountNumber(entity.getAccountNumber())
                .branchCode(entity.getBranchCode())
                .accountType(entity.getAccountType())
                .invoiceNo(entity.getInvoiceNo())
                .externalReference(entity.getExternalReference())
                .paymentReason(entity.getPaymentReason())
                .notes(entity.getNotes())
                .requestedPaymentDate(entity.getRequestedPaymentDate())
                .status(entity.getStatus())
                .approvalRequestId(entity.getApprovalRequestId())
                .paidDate(entity.getPaidDate())
                .paidReference(entity.getPaidReference())
                .paidBy(entity.getPaidBy())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .build();
    }

    public PaymentRequestEntity toEntity(PaymentRequestCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PaymentRequestEntity.builder()
                .requestNo(request.getRequestNo())
                .requestType(request.getRequestType())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .payeePartnerId(request.getPayeePartnerId())
                .payeeName(request.getPayeeName())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .bankName(request.getBankName())
                .accountHolder(request.getAccountHolder())
                .accountNumber(request.getAccountNumber())
                .branchCode(request.getBranchCode())
                .accountType(request.getAccountType())
                .invoiceNo(request.getInvoiceNo())
                .externalReference(request.getExternalReference())
                .paymentReason(request.getPaymentReason())
                .notes(request.getNotes())
                .requestedPaymentDate(request.getRequestedPaymentDate())
                .status(request.getStatus())
                .approvalRequestId(request.getApprovalRequestId())
                .paidDate(request.getPaidDate())
                .paidReference(request.getPaidReference())
                .paidBy(request.getPaidBy())
                .approvedBy(request.getApprovedBy())
                .approvedAt(request.getApprovedAt())
                .build();
    }

    public void updateEntity(PaymentRequestEntity entity, PaymentRequestUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setRequestNo(request.getRequestNo());
        entity.setRequestType(request.getRequestType());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setPayeePartnerId(request.getPayeePartnerId());
        entity.setPayeeName(request.getPayeeName());
        entity.setAmount(request.getAmount());
        entity.setCurrency(request.getCurrency());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setBankName(request.getBankName());
        entity.setAccountHolder(request.getAccountHolder());
        entity.setAccountNumber(request.getAccountNumber());
        entity.setBranchCode(request.getBranchCode());
        entity.setAccountType(request.getAccountType());
        entity.setInvoiceNo(request.getInvoiceNo());
        entity.setExternalReference(request.getExternalReference());
        entity.setPaymentReason(request.getPaymentReason());
        entity.setNotes(request.getNotes());
        entity.setRequestedPaymentDate(request.getRequestedPaymentDate());
        entity.setStatus(request.getStatus());
        entity.setApprovalRequestId(request.getApprovalRequestId());
        entity.setPaidDate(request.getPaidDate());
        entity.setPaidReference(request.getPaidReference());
        entity.setPaidBy(request.getPaidBy());
        entity.setApprovedBy(request.getApprovedBy());
        entity.setApprovedAt(request.getApprovedAt());
    }
}
