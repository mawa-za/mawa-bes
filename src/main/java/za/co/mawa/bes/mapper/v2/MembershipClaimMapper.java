package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.dto.v2.MembershipClaimCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipClaimResponseDto;
import za.co.mawa.bes.dto.v2.MembershipClaimUpdateRequestDto;

@Component
public class MembershipClaimMapper {

    public MembershipClaimResponseDto toResponse(MembershipClaimEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipClaimResponseDto.builder()
                .id(entity.getId())
                .claimNo(entity.getClaimNo())
                .membershipId(entity.getMembershipId())
                .claimType(entity.getClaimType())
                .deceasedType(entity.getDeceasedType())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .dateOfDeath(entity.getDateOfDeath())
                .claimDate(entity.getClaimDate())
                .causeOfDeath(entity.getCauseOfDeath())
                .deathCertificateNo(entity.getDeathCertificateNo())
                .claimantPartnerId(entity.getClaimantPartnerId())
                .claimAmountCents(entity.getClaimAmountCents())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .approvalRequestId(entity.getApprovalRequestId())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy())
                .paymentRequestId(entity.getPaymentRequestId())
                .payoutMethod(entity.getPayoutMethod())
                .bankName(entity.getBankName())
                .accountHolderName(entity.getAccountHolderName())
                .accountNumber(entity.getAccountNumber())
                .branchCode(entity.getBranchCode())
                .accountType(entity.getAccountType())
                .build();
    }

    public MembershipClaimEntity toEntity(MembershipClaimCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipClaimEntity.builder()
                .claimNo(request.getClaimNo())
                .membershipId(request.getMembershipId())
                .claimType(request.getClaimType())
                .deceasedType(request.getDeceasedType())
                .deceasedPartnerId(request.getDeceasedPartnerId())
                .dateOfDeath(request.getDateOfDeath())
                .claimDate(request.getClaimDate())
                .causeOfDeath(request.getCauseOfDeath())
                .deathCertificateNo(request.getDeathCertificateNo())
                .claimantPartnerId(request.getClaimantPartnerId())
                .claimAmountCents(request.getClaimAmountCents())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .notes(request.getNotes())
                .approvalRequestId(request.getApprovalRequestId())
                .approvedAt(request.getApprovedAt())
                .approvedBy(request.getApprovedBy())
                .paymentRequestId(request.getPaymentRequestId())
                .payoutMethod(request.getPayoutMethod())
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .accountNumber(request.getAccountNumber())
                .branchCode(request.getBranchCode())
                .accountType(request.getAccountType())
                .build();
    }

    public void updateEntity(MembershipClaimEntity entity, MembershipClaimUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setClaimNo(request.getClaimNo());
        entity.setMembershipId(request.getMembershipId());
        entity.setClaimType(request.getClaimType());
        entity.setDeceasedType(request.getDeceasedType());
        entity.setDeceasedPartnerId(request.getDeceasedPartnerId());
        entity.setDateOfDeath(request.getDateOfDeath());
        entity.setClaimDate(request.getClaimDate());
        entity.setCauseOfDeath(request.getCauseOfDeath());
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setClaimantPartnerId(request.getClaimantPartnerId());
        entity.setClaimAmountCents(request.getClaimAmountCents());
        entity.setStatus(request.getStatus());
        entity.setRejectionReason(request.getRejectionReason());
        entity.setNotes(request.getNotes());
        entity.setApprovalRequestId(request.getApprovalRequestId());
        entity.setApprovedAt(request.getApprovedAt());
        entity.setApprovedBy(request.getApprovedBy());
        entity.setPaymentRequestId(request.getPaymentRequestId());
        entity.setPayoutMethod(request.getPayoutMethod());
        entity.setBankName(request.getBankName());
        entity.setAccountHolderName(request.getAccountHolderName());
        entity.setAccountNumber(request.getAccountNumber());
        entity.setBranchCode(request.getBranchCode());
        entity.setAccountType(request.getAccountType());
    }
}
