package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.dto.v2.LegalCaseCreateRequestDto;
import za.co.mawa.bes.dto.v2.LegalCaseResponseDto;
import za.co.mawa.bes.dto.v2.LegalCaseUpdateRequestDto;

@Component
public class LegalCaseMapper {

    public LegalCaseResponseDto toResponse(LegalCaseEntity entity) {
        if (entity == null) {
            return null;
        }

        return LegalCaseResponseDto.builder()
                .id(entity.getId())
                .caseNo(entity.getCaseNo())
                .title(entity.getTitle())
                .clientPartnerId(entity.getClientPartnerId())
                .caseType(entity.getCaseType())
                .caseCategory(entity.getCaseCategory())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .assignedTo(entity.getAssignedTo())
                .openedDate(entity.getOpenedDate())
                .closedDate(entity.getClosedDate())
                .courtName(entity.getCourtName())
                .courtCaseNo(entity.getCourtCaseNo())
                .forumType(entity.getForumType())
                .nextAppearanceDate(entity.getNextAppearanceDate())
                .billingType(entity.getBillingType())
                .hourlyRateCents(entity.getHourlyRateCents())
                .fixedFeeCents(entity.getFixedFeeCents())
                .currency(entity.getCurrency())
                .billable(entity.getBillable())
                .totalTimeMinutes(entity.getTotalTimeMinutes())
                .totalFeesCents(entity.getTotalFeesCents())
                .totalDisbursementsCents(entity.getTotalDisbursementsCents())
                .totalBillableCents(entity.getTotalBillableCents())
                .totalBilledCents(entity.getTotalBilledCents())
                .balanceCents(entity.getBalanceCents())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public LegalCaseEntity toEntity(LegalCaseCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return LegalCaseEntity.builder()
                .caseNo(request.getCaseNo())
                .title(request.getTitle())
                .clientPartnerId(request.getClientPartnerId())
                .caseType(request.getCaseType())
                .caseCategory(request.getCaseCategory())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assignedTo(request.getAssignedTo())
                .openedDate(request.getOpenedDate())
                .closedDate(request.getClosedDate())
                .courtName(request.getCourtName())
                .courtCaseNo(request.getCourtCaseNo())
                .forumType(request.getForumType())
                .nextAppearanceDate(request.getNextAppearanceDate())
                .billingType(request.getBillingType())
                .hourlyRateCents(request.getHourlyRateCents())
                .fixedFeeCents(request.getFixedFeeCents())
                .currency(request.getCurrency())
                .billable(request.getBillable())
                .totalTimeMinutes(request.getTotalTimeMinutes())
                .totalFeesCents(request.getTotalFeesCents())
                .totalDisbursementsCents(request.getTotalDisbursementsCents())
                .totalBillableCents(request.getTotalBillableCents())
                .totalBilledCents(request.getTotalBilledCents())
                .balanceCents(request.getBalanceCents())
                .build();
    }

    public void updateEntity(LegalCaseEntity entity, LegalCaseUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCaseNo(request.getCaseNo());
        entity.setTitle(request.getTitle());
        entity.setClientPartnerId(request.getClientPartnerId());
        entity.setCaseType(request.getCaseType());
        entity.setCaseCategory(request.getCaseCategory());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setPriority(request.getPriority());
        entity.setAssignedTo(request.getAssignedTo());
        entity.setOpenedDate(request.getOpenedDate());
        entity.setClosedDate(request.getClosedDate());
        entity.setCourtName(request.getCourtName());
        entity.setCourtCaseNo(request.getCourtCaseNo());
        entity.setForumType(request.getForumType());
        entity.setNextAppearanceDate(request.getNextAppearanceDate());
        entity.setBillingType(request.getBillingType());
        entity.setHourlyRateCents(request.getHourlyRateCents());
        entity.setFixedFeeCents(request.getFixedFeeCents());
        entity.setCurrency(request.getCurrency());
        entity.setBillable(request.getBillable());
        entity.setTotalTimeMinutes(request.getTotalTimeMinutes());
        entity.setTotalFeesCents(request.getTotalFeesCents());
        entity.setTotalDisbursementsCents(request.getTotalDisbursementsCents());
        entity.setTotalBillableCents(request.getTotalBillableCents());
        entity.setTotalBilledCents(request.getTotalBilledCents());
        entity.setBalanceCents(request.getBalanceCents());
    }
}
