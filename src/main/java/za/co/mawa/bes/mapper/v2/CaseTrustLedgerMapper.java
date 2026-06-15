package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CaseTrustLedgerEntity;
import za.co.mawa.bes.dto.v2.CaseTrustLedgerCreateRequestDto;
import za.co.mawa.bes.dto.v2.CaseTrustLedgerResponseDto;
import za.co.mawa.bes.dto.v2.CaseTrustLedgerUpdateRequestDto;

@Component
public class CaseTrustLedgerMapper {

    public CaseTrustLedgerResponseDto toResponse(CaseTrustLedgerEntity entity) {
        if (entity == null) {
            return null;
        }

        return CaseTrustLedgerResponseDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .clientPartnerId(entity.getClientPartnerId())
                .currency(entity.getCurrency())
                .totalReceivedCents(entity.getTotalReceivedCents())
                .totalTransferredCents(entity.getTotalTransferredCents())
                .totalRefundedCents(entity.getTotalRefundedCents())
                .totalPaidOutCents(entity.getTotalPaidOutCents())
                .availableBalanceCents(entity.getAvailableBalanceCents())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public CaseTrustLedgerEntity toEntity(CaseTrustLedgerCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CaseTrustLedgerEntity.builder()
                .caseId(request.getCaseId())
                .clientPartnerId(request.getClientPartnerId())
                .currency(request.getCurrency())
                .totalReceivedCents(request.getTotalReceivedCents())
                .totalTransferredCents(request.getTotalTransferredCents())
                .totalRefundedCents(request.getTotalRefundedCents())
                .totalPaidOutCents(request.getTotalPaidOutCents())
                .availableBalanceCents(request.getAvailableBalanceCents())
                .build();
    }

    public void updateEntity(CaseTrustLedgerEntity entity, CaseTrustLedgerUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCaseId(request.getCaseId());
        entity.setClientPartnerId(request.getClientPartnerId());
        entity.setCurrency(request.getCurrency());
        entity.setTotalReceivedCents(request.getTotalReceivedCents());
        entity.setTotalTransferredCents(request.getTotalTransferredCents());
        entity.setTotalRefundedCents(request.getTotalRefundedCents());
        entity.setTotalPaidOutCents(request.getTotalPaidOutCents());
        entity.setAvailableBalanceCents(request.getAvailableBalanceCents());
    }
}
