package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.dto.v2.CaseDisbursementCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseDisbursementResponseDto;
import za.co.mawa.bes.dto.v2.CaseDisbursementUpdateRequestDto;

@Component
public class CaseDisbursementMapper {

    public CaseDisbursementResponseDto toResponse(CaseDisbursementEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseDisbursementResponseDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .disbursementDate(entity.getDisbursementDate())
                .disbursementType(entity.getDisbursementType())
                .description(entity.getDescription())
                .amountCents(entity.getAmountCents())
                .billable(entity.getBillable())
                .billed(entity.getBilled())
                .invoiceId(entity.getInvoiceId())
                .paidFromTrust(entity.getPaidFromTrust())
                .trustTransactionId(entity.getTrustTransactionId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public CaseDisbursementEntity toEntity(CaseDisbursementCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseDisbursementEntity.builder()
                .caseId(request.getCaseId())
                .disbursementDate(request.getDisbursementDate())
                .disbursementType(request.getDisbursementType())
                .description(request.getDescription())
                .amountCents(request.getAmountCents())
                .billable(request.getBillable())
                .billed(request.getBilled())
                .invoiceId(request.getInvoiceId())
                .paidFromTrust(request.getPaidFromTrust())
                .trustTransactionId(request.getTrustTransactionId())
                .build();
    }

    public void updateEntity(CaseDisbursementEntity entity, CaseDisbursementUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCaseId(request.getCaseId());
        entity.setDisbursementDate(request.getDisbursementDate());
        entity.setDisbursementType(request.getDisbursementType());
        entity.setDescription(request.getDescription());
        entity.setAmountCents(request.getAmountCents());
        entity.setBillable(request.getBillable());
        entity.setBilled(request.getBilled());
        entity.setInvoiceId(request.getInvoiceId());
        entity.setPaidFromTrust(request.getPaidFromTrust());
        entity.setTrustTransactionId(request.getTrustTransactionId());
    }
}
