package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.membership.claim.*;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimLinkEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.v2.MembershipClaimLinkRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.NumberRangeService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MembershipClaimService {
    @Autowired
    NumberRangeService numberRangeService;
    private final MembershipClaimRepository claimRepository;
    private final MembershipClaimLinkRepository claimLinkRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipDependentRepository membershipDependentRepository;

    public MembershipClaimService(
            MembershipClaimRepository claimRepository,
            MembershipClaimLinkRepository claimLinkRepository,
            MembershipRepository membershipRepository,
            MembershipDependentRepository membershipDependentRepository
    ) {
        this.claimRepository = claimRepository;
        this.claimLinkRepository = claimLinkRepository;
        this.membershipRepository = membershipRepository;
        this.membershipDependentRepository = membershipDependentRepository;
    }

    @Transactional
    public MembershipClaimResponse create(MembershipClaimCreateRequest request, String userId) {
        validateCreateRequest(request);

        MembershipEntity membership = membershipRepository.findById(request.getMembershipId())
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + request.getMembershipId()));

        validateDeceasedAgainstMembership(
                request.getMembershipId(),
                membership.getMemberId(),
                request.getDeceasedType(),
                request.getDeceasedPartnerId()
        );

        MembershipClaimEntity entity = new MembershipClaimEntity();
        try {
            entity.setClaimNo(numberRangeService.generateNumber("CLAIM"));
        } catch (NumberRangeObjectNotFound e) {
            throw new RuntimeException(e);
        }
        entity.setMembershipId(request.getMembershipId());
        entity.setClaimType(request.getClaimType());
        entity.setDeceasedType(request.getDeceasedType());
        entity.setDeceasedPartnerId(request.getDeceasedPartnerId());
        entity.setDateOfDeath(request.getDateOfDeath());
        entity.setClaimDate(request.getClaimDate() != null ? request.getClaimDate() : LocalDate.now());
        entity.setCauseOfDeath(request.getCauseOfDeath());
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setClaimantPartnerId(request.getClaimantPartnerId());
        entity.setClaimAmountCents(request.getClaimAmountCents() != null ? request.getClaimAmountCents() : 0L);
        entity.setNotes(request.getNotes());
        entity.setStatus(Boolean.TRUE.equals(request.getSubmit())
                ? MembershipClaimStatus.SUBMITTED
                : MembershipClaimStatus.DRAFT);
        entity.setCreatedBy(userId);

        MembershipClaimEntity saved = claimRepository.save(entity);

        if (saved.getClaimType() == MembershipClaimType.COMBINATION
                && request.getLinkedClaimIds() != null
                && !request.getLinkedClaimIds().isEmpty()) {
            attachClaimsToCombination(saved.getId(), request.getLinkedClaimIds(), userId);
        }

        return getById(saved.getId());
    }

    private void validateCashClaimPaymentDetails(MembershipClaimCreateRequest request) {
        if (!"CASH".equalsIgnoreCase(String.valueOf(request.getClaimType()))) {
            return;
        }

        if (request.getPayoutMethod() == null || request.getPayoutMethod().isBlank()) {
            throw new RuntimeException("Payout method is required for CASH claims");
        }

        if ("EFT".equalsIgnoreCase(request.getPayoutMethod())) {
            if (request.getBankName() == null || request.getBankName().isBlank()) {
                throw new RuntimeException("Bank name is required for EFT payout");
            }

            if (request.getAccountHolderName() == null || request.getAccountHolderName().isBlank()) {
                throw new RuntimeException("Account holder name is required for EFT payout");
            }

            if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
                throw new RuntimeException("Account number is required for EFT payout");
            }

            if (request.getBranchCode() == null || request.getBranchCode().isBlank()) {
                throw new RuntimeException("Branch code is required for EFT payout");
            }

            if (request.getAccountType() == null || request.getAccountType().isBlank()) {
                throw new RuntimeException("Account type is required for EFT payout");
            }
        }
    }

    public List<MembershipClaimResponse> getAll() {
        return claimRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MembershipClaimResponse getById(String id) {
        return toResponse(getClaimEntity(id));
    }

    public MembershipClaimResponse getByClaimNo(String claimNo) {
        MembershipClaimEntity entity = claimRepository.findByClaimNo(claimNo)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimNo));

        return toResponse(entity);
    }

    public List<MembershipClaimResponse> getByMembershipId(String membershipId) {
        return claimRepository.findByMembershipIdOrderByCreatedAtDesc(membershipId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MembershipClaimResponse> getByStatus(MembershipClaimStatus status) {
        return claimRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MembershipClaimResponse> getByClaimType(MembershipClaimType claimType) {
        return claimRepository.findByClaimTypeOrderByCreatedAtDesc(claimType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MembershipClaimResponse> getByDeceasedPartnerId(String deceasedPartnerId) {
        return claimRepository.findByDeceasedPartnerIdOrderByCreatedAtDesc(deceasedPartnerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MembershipClaimResponse update(String id, MembershipClaimUpdateRequest request, String userId) {
        MembershipClaimEntity entity = getClaimEntity(id);

        if (entity.getStatus() != MembershipClaimStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT claims can be updated.");
        }

        if (request.getDateOfDeath() != null) {
            if (request.getDateOfDeath().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of death cannot be in the future.");
            }
            entity.setDateOfDeath(request.getDateOfDeath());
        }

        if (request.getClaimDate() != null) {
            entity.setClaimDate(request.getClaimDate());
        }

        if (request.getClaimAmountCents() != null) {
            if (request.getClaimAmountCents() < 0) {
                throw new IllegalArgumentException("Claim amount cannot be negative.");
            }
            entity.setClaimAmountCents(request.getClaimAmountCents());
        }

        entity.setCauseOfDeath(request.getCauseOfDeath());
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setClaimantPartnerId(request.getClaimantPartnerId());
        entity.setNotes(request.getNotes());
        entity.setUpdatedBy(userId);

        return toResponse(claimRepository.save(entity));
    }

    @Transactional
    public MembershipClaimResponse submit(String id, String userId) {
        MembershipClaimEntity entity = getClaimEntity(id);

        if (entity.getStatus() != MembershipClaimStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT claims can be submitted.");
        }

        if (entity.getClaimType() == MembershipClaimType.COMBINATION) {
            validateCombinationReadyForSubmit(entity);
        }

        entity.setStatus(MembershipClaimStatus.SUBMITTED);
        entity.setUpdatedBy(userId);

        return toResponse(claimRepository.save(entity));
    }

    @Transactional
    public MembershipClaimResponse cancel(String id, String userId) {
        MembershipClaimEntity entity = getClaimEntity(id);

        if (entity.getStatus() == MembershipClaimStatus.APPROVED
                || entity.getStatus() == MembershipClaimStatus.PAID) {
            throw new IllegalArgumentException("Approved or paid claims cannot be cancelled from claim module.");
        }

        entity.setStatus(MembershipClaimStatus.CANCELLED);
        entity.setUpdatedBy(userId);

        return toResponse(claimRepository.save(entity));
    }

    @Transactional
    public MembershipClaimResponse attachClaimsToCombination(
            String parentClaimId,
            MembershipClaimsAttachRequest request,
            String userId
    ) {
        return attachClaimsToCombination(parentClaimId, request.getClaimIds(), userId);
    }

    @Transactional
    public MembershipClaimResponse attachClaimsToCombination(
            String parentClaimId,
            List<String> linkedClaimIds,
            String userId
    ) {
        MembershipClaimEntity parentClaim = getClaimEntity(parentClaimId);

        if (parentClaim.getClaimType() != MembershipClaimType.COMBINATION) {
            throw new IllegalArgumentException("Only COMBINATION claims can have linked claims.");
        }

        if (parentClaim.getStatus() != MembershipClaimStatus.DRAFT
                && parentClaim.getStatus() != MembershipClaimStatus.SUBMITTED) {
            throw new IllegalArgumentException("Cannot attach claims when parent claim is in status: " + parentClaim.getStatus());
        }

        if (linkedClaimIds == null || linkedClaimIds.isEmpty()) {
            throw new IllegalArgumentException("At least one linked claim is required.");
        }

        Set<String> uniqueIds = new HashSet<>(linkedClaimIds);

        for (String linkedClaimId : uniqueIds) {
            attachSingleClaim(parentClaim, linkedClaimId, userId);
        }

        return getById(parentClaimId);
    }

    @Transactional
    public MembershipClaimResponse detachClaimFromCombination(
            String parentClaimId,
            String linkedClaimId
    ) {
        MembershipClaimEntity parentClaim = getClaimEntity(parentClaimId);

        if (parentClaim.getClaimType() != MembershipClaimType.COMBINATION) {
            throw new IllegalArgumentException("Only COMBINATION claims can have linked claims.");
        }

        if (parentClaim.getStatus() != MembershipClaimStatus.DRAFT
                && parentClaim.getStatus() != MembershipClaimStatus.SUBMITTED) {
            throw new IllegalArgumentException("Cannot detach claims when parent claim is in status: " + parentClaim.getStatus());
        }

        claimLinkRepository.deleteByParentClaimIdAndLinkedClaimId(parentClaimId, linkedClaimId);

        return getById(parentClaimId);
    }

    private void attachSingleClaim(
            MembershipClaimEntity parentClaim,
            String linkedClaimId,
            String userId
    ) {
        if (parentClaim.getId().equals(linkedClaimId)) {
            throw new IllegalArgumentException("A combination claim cannot link to itself.");
        }

        MembershipClaimEntity linkedClaim = getClaimEntity(linkedClaimId);

        if (linkedClaim.getClaimType() != MembershipClaimType.COMBINATION) {
            throw new IllegalArgumentException("Only COMBINATION claims can be attached to a COMBINATION claim.");
        }

        if (parentClaim.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT
                || linkedClaim.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT) {
            throw new IllegalArgumentException("All combination claims must have deceased type DEPENDENT.");
        }

        if (!parentClaim.getDeceasedPartnerId().equals(linkedClaim.getDeceasedPartnerId())) {
            throw new IllegalArgumentException("Linked claim deceased partner does not match parent combination claim.");
        }

        if (parentClaim.getMembershipId().equals(linkedClaim.getMembershipId())) {
            throw new IllegalArgumentException("Linked claim cannot belong to the same membership as the parent claim.");
        }

        if (linkedClaim.getStatus() == MembershipClaimStatus.CANCELLED
                || linkedClaim.getStatus() == MembershipClaimStatus.REJECTED
                || linkedClaim.getStatus() == MembershipClaimStatus.APPROVED
                || linkedClaim.getStatus() == MembershipClaimStatus.PAID) {
            throw new IllegalArgumentException("Cannot link claim in status: " + linkedClaim.getStatus());
        }

        if (claimLinkRepository.existsByLinkedClaimId(linkedClaim.getId())) {
            throw new IllegalArgumentException("Linked claim already belongs to another combination.");
        }

        if (claimLinkRepository.existsByParentClaimId(linkedClaim.getId())) {
            throw new IllegalArgumentException("Linked claim is already a parent combination claim.");
        }

        if (claimLinkRepository.existsByLinkedClaimId(parentClaim.getId())) {
            throw new IllegalArgumentException("Parent claim is already linked to another combination.");
        }

        if (claimLinkRepository.existsByParentClaimIdAndLinkedClaimId(parentClaim.getId(), linkedClaim.getId())) {
            return;
        }

        MembershipClaimLinkEntity link = new MembershipClaimLinkEntity();
        link.setParentClaimId(parentClaim.getId());
        link.setLinkedClaimId(linkedClaim.getId());
        link.setCreatedBy(userId);

        claimLinkRepository.save(link);
    }

    private void validateCreateRequest(MembershipClaimCreateRequest request) {
        if (!StringUtils.hasText(request.getMembershipId())) {
            throw new IllegalArgumentException("Membership ID is required.");
        }

        if (request.getClaimType() == null) {
            throw new IllegalArgumentException("Claim type is required.");
        }

        if (request.getDeceasedType() == null) {
            throw new IllegalArgumentException("Deceased type is required.");
        }

        if (!StringUtils.hasText(request.getDeceasedPartnerId())) {
            throw new IllegalArgumentException("Deceased partner ID is required.");
        }

        if (request.getDateOfDeath() == null) {
            throw new IllegalArgumentException("Date of death is required.");
        }

        if (request.getDateOfDeath().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of death cannot be in the future.");
        }

        if (request.getClaimAmountCents() != null && request.getClaimAmountCents() < 0) {
            throw new IllegalArgumentException("Claim amount cannot be negative.");
        }

        if (request.getClaimType() == MembershipClaimType.COMBINATION
                && request.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT) {
            throw new IllegalArgumentException("COMBINATION claim deceased must be DEPENDENT.");
        }

        if (request.getClaimType() != MembershipClaimType.COMBINATION
                && request.getLinkedClaimIds() != null
                && !request.getLinkedClaimIds().isEmpty()) {
            throw new IllegalArgumentException("Linked claims are only allowed for COMBINATION claims.");
        }
        validateCashClaimPaymentDetails(request);
    }

    private void validateDeceasedAgainstMembership(
            String membershipId,
            String mainMemberPartnerId,
            MembershipClaimDeceasedType deceasedType,
            String deceasedPartnerId
    ) {
        if (deceasedType == MembershipClaimDeceasedType.MAIN_MEMBER) {
            if (!deceasedPartnerId.equals(mainMemberPartnerId)) {
                throw new IllegalArgumentException("For main member claims, deceased partner must match membership member.");
            }

            return;
        }

        if (deceasedType == MembershipClaimDeceasedType.DEPENDENT) {
            if (deceasedPartnerId.equals(mainMemberPartnerId)) {
                throw new IllegalArgumentException("Dependent claim cannot use the main member partner ID.");
            }

            boolean linkedDependent = membershipDependentRepository
                    .existsByMembershipIdAndDependentPartnerId(membershipId, deceasedPartnerId);

            if (!linkedDependent) {
                throw new IllegalArgumentException("Deceased partner is not linked as a dependent on this membership.");
            }
        }
    }

    private void validateCombinationReadyForSubmit(MembershipClaimEntity parentClaim) {
        if (parentClaim.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT) {
            throw new IllegalArgumentException("COMBINATION claim deceased must be DEPENDENT.");
        }

        List<MembershipClaimLinkEntity> links =
                claimLinkRepository.findByParentClaimIdOrderByCreatedAtAsc(parentClaim.getId());

        if (links.isEmpty()) {
            throw new IllegalArgumentException("COMBINATION claim requires at least one linked COMBINATION claim before submission.");
        }

        for (MembershipClaimLinkEntity link : links) {
            MembershipClaimEntity linkedClaim = getClaimEntity(link.getLinkedClaimId());

            if (linkedClaim.getClaimType() != MembershipClaimType.COMBINATION) {
                throw new IllegalArgumentException("All linked claims must be COMBINATION claims.");
            }

            if (linkedClaim.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT) {
                throw new IllegalArgumentException("All linked claims must have deceased type DEPENDENT.");
            }

            if (!parentClaim.getDeceasedPartnerId().equals(linkedClaim.getDeceasedPartnerId())) {
                throw new IllegalArgumentException("All linked claims must have the same deceased partner.");
            }
        }
    }

    private MembershipClaimEntity getClaimEntity(String id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
    }

    private MembershipClaimResponse toResponse(MembershipClaimEntity entity) {
        List<MembershipClaimLinkEntity> links =
                claimLinkRepository.findByParentClaimIdOrderByCreatedAtAsc(entity.getId());

        List<LinkedMembershipClaimResponse> linkedClaims = links.stream()
                .map(link -> {
                    MembershipClaimEntity linkedClaim = getClaimEntity(link.getLinkedClaimId());

                    return new LinkedMembershipClaimResponse()
                            .setLinkId(link.getId())
                            .setClaimId(linkedClaim.getId())
                            .setClaimNo(linkedClaim.getClaimNo())
                            .setMembershipId(linkedClaim.getMembershipId())
                            .setClaimType(linkedClaim.getClaimType())
                            .setClaimAmountCents(linkedClaim.getClaimAmountCents())
                            .setStatus(linkedClaim.getStatus());
                })
                .collect(Collectors.toList());

        long linkedTotal = linkedClaims.stream()
                .mapToLong(linked -> linked.getClaimAmountCents() != null ? linked.getClaimAmountCents() : 0L)
                .sum();

        return new MembershipClaimResponse()
                .setId(entity.getId())
                .setClaimNo(entity.getClaimNo())
                .setMembershipId(entity.getMembershipId())
                .setClaimType(entity.getClaimType())
                .setDeceasedType(entity.getDeceasedType())
                .setDeceasedPartnerId(entity.getDeceasedPartnerId())
                .setDateOfDeath(entity.getDateOfDeath())
                .setClaimDate(entity.getClaimDate())
                .setCauseOfDeath(entity.getCauseOfDeath())
                .setDeathCertificateNo(entity.getDeathCertificateNo())
                .setClaimantPartnerId(entity.getClaimantPartnerId())
                .setClaimAmountCents(entity.getClaimAmountCents())
                .setCombinedClaimAmountCents(entity.getClaimAmountCents() + linkedTotal)
                .setStatus(entity.getStatus())
                .setRejectionReason(entity.getRejectionReason())
                .setNotes(entity.getNotes())
                .setParentCombinationClaim(claimLinkRepository.existsByParentClaimId(entity.getId()))
                .setLinkedToCombinationClaim(claimLinkRepository.existsByLinkedClaimId(entity.getId()))
                .setCreatedAt(entity.getCreatedAt())
                .setCreatedBy(entity.getCreatedBy())
                .setUpdatedAt(entity.getUpdatedAt())
                .setUpdatedBy(entity.getUpdatedBy())
                .setLinkedClaims(linkedClaims)
                .setPayoutMethod(entity.getPayoutMethod())
                .setBankName(entity.getBankName())
                .setAccountHolderName(entity.getAccountHolderName())
                .setAccountNumber(entity.getAccountNumber())
                .setBranchCode(entity.getBranchCode())
                .setAccountType(entity.getAccountType());

    }
}
