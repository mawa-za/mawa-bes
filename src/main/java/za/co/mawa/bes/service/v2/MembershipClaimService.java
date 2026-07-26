package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.membership.claim.*;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.MembershipDependentStatus;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimLinkRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MembershipClaimService {
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    ClaimFormGenerationService claimFormGenerationService;
    private final MembershipClaimRepository claimRepository;
    private final MembershipClaimLinkRepository claimLinkRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipDependentRepository membershipDependentRepository;
    private final PartnerRepository partnerRepository;
    private final NumberAllocationService numberAllocationService;
    private final JdbcTemplate jdbcTemplate;
    private final MembershipChangeService membershipChangeService;
    private final MembershipPlanClaimPayoutService membershipPlanClaimPayoutService;
    private final ClaimTypeConfigurationService claimTypeConfigurationService;
    private final ReferenceDataValidationService referenceDataValidationService;
    private final UniversalBranchCodeService universalBranchCodeService;

    public MembershipClaimService(
            MembershipClaimRepository claimRepository,
            MembershipClaimLinkRepository claimLinkRepository,
            MembershipRepository membershipRepository,
            MembershipPlanRepository membershipPlanRepository,
            MembershipDependentRepository membershipDependentRepository,
            PartnerRepository partnerRepository,
            NumberAllocationService numberAllocationService,
            JdbcTemplate jdbcTemplate,
            MembershipChangeService membershipChangeService,
            MembershipPlanClaimPayoutService membershipPlanClaimPayoutService,
            ClaimTypeConfigurationService claimTypeConfigurationService,
            ReferenceDataValidationService referenceDataValidationService,
            UniversalBranchCodeService universalBranchCodeService
    ) {
        this.claimRepository = claimRepository;
        this.claimLinkRepository = claimLinkRepository;
        this.membershipRepository = membershipRepository;
        this.membershipPlanRepository = membershipPlanRepository;
        this.membershipDependentRepository = membershipDependentRepository;
        this.partnerRepository = partnerRepository;
        this.numberAllocationService = numberAllocationService;
        this.jdbcTemplate = jdbcTemplate;
        this.membershipChangeService = membershipChangeService;
        this.membershipPlanClaimPayoutService = membershipPlanClaimPayoutService;
        this.claimTypeConfigurationService = claimTypeConfigurationService;
        this.referenceDataValidationService = referenceDataValidationService;
        this.universalBranchCodeService = universalBranchCodeService;
    }

    @Transactional
    public MembershipClaimResponse create(MembershipClaimCreateRequest request, String userId) {
        validateCreateRequest(request);
        claimTypeConfigurationService.requireEnabled(request.getClaimType());
        String causeOfDeath = referenceDataValidationService.optionalOption(
                "CAUSE-OF-DEATH", request.getCauseOfDeath(), "Cause of death");

        MembershipEntity membership = membershipRepository.findById(request.getMembershipId())
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + request.getMembershipId()));

        validateDeceasedAgainstMembership(
                request.getMembershipId(),
                membership.getMemberId(),
                request.getDeceasedType(),
                request.getDeceasedPartnerId()
        );

        MembershipClaimEntity entity = new MembershipClaimEntity();
        entity.setClaimNo(generateMembershipClaimNo());
        entity.setMembershipId(request.getMembershipId());
        entity.setClaimType(request.getClaimType());
        LocalDate coverageEventDate = request.getDateOfDeath() != null ? request.getDateOfDeath()
                : request.getClaimDate() != null ? request.getClaimDate() : LocalDate.now();
        String coveragePlanId = membershipChangeService.resolveCoveragePlanId(
                request.getMembershipId(), coverageEventDate, userId);
        entity.setCoveragePlanId(coveragePlanId);
        entity.setCoverageEventDate(coverageEventDate);
        entity.setDeceasedType(request.getDeceasedType());
        entity.setDeceasedPartnerId(request.getDeceasedPartnerId());
        entity.setDateOfDeath(request.getDateOfDeath());
        entity.setClaimDate(request.getClaimDate() != null ? request.getClaimDate() : LocalDate.now());
        entity.setBurialDate(request.getBurialDate());
        entity.setCauseOfDeath(causeOfDeath);
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setClaimantPartnerId(request.getClaimantPartnerId());
        entity.setClaimAmountCents(resolvePlanBenefitAmount(
                membership,
                request.getMembershipId(),
                request.getDeceasedPartnerId(),
                coveragePlanId,
                request.getClaimType()));
        entity.setNotes(request.getNotes());
        if (request.getClaimType() == MembershipClaimType.CASH) {
            za.co.mawa.bes.enums.PaymentMethod payoutMethod = za.co.mawa.bes.enums.PaymentMethod.valueOf(
                    request.getPayoutMethod().trim().toUpperCase());
            entity.setPayoutMethod(payoutMethod);
            if (payoutMethod == za.co.mawa.bes.enums.PaymentMethod.EFT) {
                entity.setBankName(referenceDataValidationService.requireOption(
                        "BANK-NAME", request.getBankName(), "Bank name"));
                entity.setAccountHolderName(request.getAccountHolderName().trim());
                entity.setAccountNumber(request.getAccountNumber().trim());
                entity.setBranchCode(universalBranchCodeService.resolve(entity.getBankName()));
                String accountType = referenceDataValidationService.requireOption(
                        "BANK-ACCOUNT-TYPE", request.getAccountType(), "Bank account type");
                entity.setAccountType(za.co.mawa.bes.enums.BankAccountType.valueOf(accountType.toUpperCase()));
            }
        }
        entity.setStatus(Boolean.TRUE.equals(request.getSubmit())
                ? MembershipClaimStatus.SUBMITTED
                : MembershipClaimStatus.DRAFT);
        entity.setCreatedBy(userId);

        MembershipClaimEntity saved = claimRepository.save(entity);
        markDeceasedOnMembership(saved, userId);

        if (saved.getStatus() == MembershipClaimStatus.SUBMITTED) {
            claimFormGenerationService.generateForSubmittedClaim(saved.getId());
        }

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
            referenceDataValidationService.requireOption("BANK-NAME", request.getBankName(), "Bank name");

            if (!StringUtils.hasText(request.getAccountHolderName())) {
                throw new IllegalArgumentException("Account holder name is required for EFT payout");
            }

            if (!StringUtils.hasText(request.getAccountNumber()) || !request.getAccountNumber().trim().matches("\\d{5,20}")) {
                throw new IllegalArgumentException("Account number must contain 5 to 20 numeric digits");
            }

            universalBranchCodeService.resolve(request.getBankName());

            referenceDataValidationService.requireOption("BANK-ACCOUNT-TYPE", request.getAccountType(), "Bank account type");
        }
    }

    public List<MembershipClaimResponse> getAll() {
        return claimRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Slice<MembershipClaimListItemResponse> getPage(
            MembershipClaimStatus status,
            String query,
            Pageable pageable
    ) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        return claimRepository.searchPage(status, normalizedQuery, pageable)
                .map(this::toListItemResponse);
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
        if (request.getBurialDate() != null) {
            validateBurialDate(entity.getDateOfDeath(), request.getBurialDate());
            entity.setBurialDate(request.getBurialDate());
        }

        MembershipEntity membership = membershipRepository.findById(entity.getMembershipId())
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + entity.getMembershipId()));
        LocalDate coverageEventDate = entity.getDateOfDeath() != null
                ? entity.getDateOfDeath()
                : entity.getClaimDate() != null ? entity.getClaimDate() : LocalDate.now();
        String coveragePlanId = membershipChangeService.resolveCoveragePlanId(
                entity.getMembershipId(), coverageEventDate, userId);
        entity.setCoveragePlanId(coveragePlanId);
        entity.setCoverageEventDate(coverageEventDate);

        entity.setClaimAmountCents(resolvePlanBenefitAmount(
                membership,
                entity.getMembershipId(),
                entity.getDeceasedPartnerId(),
                coveragePlanId,
                entity.getClaimType()));

        entity.setCauseOfDeath(referenceDataValidationService.optionalOption(
                "CAUSE-OF-DEATH", request.getCauseOfDeath(), "Cause of death"));
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setClaimantPartnerId(request.getClaimantPartnerId());
        entity.setNotes(request.getNotes());
        entity.setUpdatedBy(userId);

        MembershipClaimEntity saved = claimRepository.save(entity);
        markDeceasedOnMembership(saved, userId);
        refreshLinkedFuneralServiceStatus(saved.getId());
        return toResponse(saved);
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

        MembershipClaimEntity saved = claimRepository.save(entity);
        claimFormGenerationService.generateForSubmittedClaim(saved.getId());
        refreshLinkedFuneralServiceStatus(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public MembershipClaimResponse markApprovedFromWorkflow(String id, String userId) {
        MembershipClaimEntity entity = getClaimEntity(id);
        if (entity.getStatus() == MembershipClaimStatus.PAID
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PROCESSING
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PENDING) {
            return toResponse(entity);
        }
        entity.setStatus(entity.getClaimType() == MembershipClaimType.CASH
                ? MembershipClaimStatus.PAYMENT_PENDING
                : MembershipClaimStatus.APPROVED);
        if (entity.getApprovedAmountCents() == null || entity.getApprovedAmountCents() <= 0) {
            entity.setApprovedAmountCents(entity.getClaimAmountCents() == null ? 0L : entity.getClaimAmountCents());
        }
        entity.setApprovedBy(userId);
        entity.setApprovedAt(java.time.LocalDateTime.now());
        entity.setUpdatedBy(userId);
        MembershipClaimEntity saved = claimRepository.save(entity);
        refreshLinkedFuneralServiceStatus(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public MembershipClaimResponse linkApproval(ApprovalRequestEntity approvalRequest, String userId) {
        MembershipClaimEntity entity = getClaimEntity(approvalRequest.getReferenceId());
        entity.setApprovalRequestId(approvalRequest.getId());
        entity.setUpdatedBy(userId);
        return toResponse(claimRepository.save(entity));
    }

    @Transactional
    public MembershipClaimResponse linkPaymentRequest(PaymentRequestResponse paymentRequestResponse, String userId) {
        MembershipClaimEntity entity = getClaimEntity(paymentRequestResponse.getSourceId());
        entity.setPaymentRequestId(paymentRequestResponse.getId());
        entity.setUpdatedBy(userId);
        return toResponse(claimRepository.save(entity));
    }

    @Transactional
    public void markPaymentProcessing(String claimId, String userId) {
        MembershipClaimEntity entity = getClaimEntity(claimId);
        if (entity.getStatus() == MembershipClaimStatus.PAID) return;
        entity.setStatus(MembershipClaimStatus.PAYMENT_PROCESSING);
        entity.setUpdatedBy(userId);
        claimRepository.save(entity);
        refreshLinkedFuneralServiceStatus(entity.getId());
    }

    @Transactional
    public void markPaymentPaid(String claimId, String userId) {
        MembershipClaimEntity entity = getClaimEntity(claimId);
        entity.setStatus(MembershipClaimStatus.PAID);
        entity.setUpdatedBy(userId);
        claimRepository.save(entity);
        refreshLinkedFuneralServiceStatus(entity.getId());
    }

    @Transactional
    public void markPaymentFailed(String claimId, String reason, String userId) {
        MembershipClaimEntity entity = getClaimEntity(claimId);
        if (entity.getStatus() == MembershipClaimStatus.PAID) return;
        entity.setStatus(MembershipClaimStatus.PAYMENT_FAILED);
        entity.setNotes(appendNote(entity.getNotes(), "Payment failed: " + (reason == null ? "Unknown bank response" : reason)));
        entity.setUpdatedBy(userId);
        claimRepository.save(entity);
        refreshLinkedFuneralServiceStatus(entity.getId());
    }

    private void markDeceasedOnMembership(MembershipClaimEntity claim, String userId) {
        if (claim.getDeceasedType() != MembershipClaimDeceasedType.DEPENDENT
                || !StringUtils.hasText(claim.getDeceasedPartnerId())) {
            return;
        }

        membershipDependentRepository
                .findFirstByMembershipIdAndDependentPartnerIdOrderByCreatedAtDesc(
                        claim.getMembershipId(), claim.getDeceasedPartnerId())
                .ifPresent(dependent -> {
                    dependent.setActive(false);
                    dependent.setStatus(MembershipDependentStatus.DECEASED);
                    dependent.setDeceasedDate(claim.getDateOfDeath());
                    dependent.setEffectiveTo(claim.getDateOfDeath());
                    dependent.setStatusReason("Deceased claim " + claim.getClaimNo());
                    dependent.setUpdatedBy(userId);
                    membershipDependentRepository.save(dependent);
                });

        partnerRepository.findById(claim.getDeceasedPartnerId()).ifPresent(partner -> {
            if (!Status.DECEASED.equalsIgnoreCase(partner.getStatus())) {
                partner.setStatus(Status.DECEASED);
                partner.setStatusReason("CLAIM");
                partnerRepository.save(partner);
            }
        });
    }

    private String appendNote(String existing, String note) {
        if (!StringUtils.hasText(existing)) return note;
        if (existing.contains(note)) return existing;
        return existing + "\n" + note;
    }

    @Transactional
    public MembershipClaimResponse cancel(String id, String userId) {
        MembershipClaimEntity entity = getClaimEntity(id);

        if (entity.getStatus() == MembershipClaimStatus.APPROVED
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PENDING
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PROCESSING
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


    private String generateMembershipClaimNo() {
        try {
            return numberAllocationService.allocateNumber("MEMBERSHIP_CLAIM");
        } catch (Exception ignored) {
            try {
                return numberAllocationService.allocateNumber("CLAIM");
            } catch (Exception ignoredAgain) {
                try {
                    return numberRangeService.generateNumber("CLAIM");
                } catch (NumberRangeObjectNotFound e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Long resolveApprovedAmount(MembershipClaimEntity entity) {
        if (entity.getStatus() == MembershipClaimStatus.APPROVED
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PENDING
                || entity.getStatus() == MembershipClaimStatus.PAYMENT_PROCESSING
                || entity.getStatus() == MembershipClaimStatus.PAID) {
            return entity.getApprovedAmountCents() == null ? entity.getClaimAmountCents() : entity.getApprovedAmountCents();
        }
        return entity.getApprovedAmountCents() == null ? 0L : entity.getApprovedAmountCents();
    }

    private void refreshLinkedFuneralServiceStatus(String membershipClaimId) {
        try {
            jdbcTemplate.update("""
                    UPDATE funeral_service fs
                       SET fs.status = CASE
                           WHEN EXISTS (
                               SELECT 1
                                 FROM funeral_service_claim fsc
                                 JOIN membership_claim mc ON mc.id = fsc.membership_claim_id
                                WHERE fsc.funeral_service_id = fs.id
                                  AND mc.status IN ('DRAFT', 'SUBMITTED')
                           ) THEN 'CLAIMS_INITIATED'
                           ELSE 'CLAIMS_RESOLVED'
                       END,
                       fs.updated_at = CURRENT_TIMESTAMP
                     WHERE fs.id IN (SELECT funeral_service_id FROM funeral_service_claim WHERE membership_claim_id = ?)
                    """, membershipClaimId);
        } catch (Exception ignored) {
            // Funeral linkage is optional for normal membership claims.
        }
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

        validateBurialDate(request.getDateOfDeath(), request.getBurialDate());

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

    public java.util.Map<String, Object> resolveBenefit(
            String membershipId,
            MembershipClaimType claimType,
            String deceasedPartnerId,
            LocalDate eventDate,
            String userId
    ) {
        claimTypeConfigurationService.requireEnabled(claimType);
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        String deceased = StringUtils.hasText(deceasedPartnerId) ? deceasedPartnerId : membership.getMemberId();
        String planId = membershipChangeService.resolveCoveragePlanId(
                membershipId, eventDate == null ? LocalDate.now() : eventDate, userId);
        Long amount = resolvePlanBenefitAmount(membership, membershipId, deceased, planId, claimType);
        return java.util.Map.of(
                "membershipId", membershipId,
                "coveragePlanId", planId,
                "claimType", claimType.name(),
                "claimAmountCents", amount
        );
    }

    private Long resolvePlanBenefitAmount(
            MembershipEntity membership,
            String membershipId,
            String deceasedPartnerId,
            String coveragePlanId,
            MembershipClaimType claimType
    ) {
        za.co.mawa.bes.enums.DependentType payoutDependentType = za.co.mawa.bes.enums.DependentType.MAIN_MEMBER;
        if (!membership.getMemberId().equals(deceasedPartnerId)) {
            payoutDependentType = membershipDependentRepository.findByMembershipId(membershipId).stream()
                    .filter(item -> deceasedPartnerId.equals(item.getDependentPartnerId()))
                    .map(MembershipDependentEntity::getDependentType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Deceased partner is not linked to the membership."));
        }
        return membershipPlanClaimPayoutService.resolvePayoutAmountCents(
                coveragePlanId, claimType, payoutDependentType);
    }

    private void validateBurialDate(LocalDate dateOfDeath, LocalDate burialDate) {
        if (burialDate != null && dateOfDeath != null && burialDate.isBefore(dateOfDeath)) {
            throw new IllegalArgumentException("Burial date cannot be before the date of death.");
        }
    }

    private MembershipClaimEntity getClaimEntity(String id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
    }

    private MembershipClaimListItemResponse toListItemResponse(MembershipClaimEntity entity) {
        MembershipReference membership = membershipReference(entity.getMembershipId());
        PartnerReference deceased = partnerReference(entity.getDeceasedPartnerId());
        PartnerReference claimant = partnerReference(entity.getClaimantPartnerId());

        return MembershipClaimListItemResponse.builder()
                .id(entity.getId())
                .claimNo(entity.getClaimNo())
                .membershipId(entity.getMembershipId())
                .membershipNo(membership.membershipNo())
                .memberName(membership.member().name())
                .memberNumber(membership.member().number())
                .memberIdentityNumber(membership.member().identityNumber())
                .deceasedName(deceased.name())
                .deceasedNumber(deceased.number())
                .deceasedIdentityNumber(deceased.identityNumber())
                .claimantName(claimant.name())
                .claimType(entity.getClaimType())
                .coveragePlanId(entity.getCoveragePlanId())
                .coverageEventDate(entity.getCoverageEventDate())
                .deceasedType(entity.getDeceasedType())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .dateOfDeath(entity.getDateOfDeath())
                .claimDate(entity.getClaimDate())
                .claimantPartnerId(entity.getClaimantPartnerId())
                .claimAmountCents(entity.getClaimAmountCents())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private MembershipClaimResponse toResponse(MembershipClaimEntity entity) {
        MembershipReference membership = membershipReference(entity.getMembershipId());
        PartnerReference deceased = partnerReference(entity.getDeceasedPartnerId());
        PartnerReference claimant = partnerReference(entity.getClaimantPartnerId());

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
                .setMembershipNo(membership.membershipNo())
                .setMemberName(membership.member().name())
                .setMemberNumber(membership.member().number())
                .setMemberIdentityNumber(membership.member().identityNumber())
                .setDeceasedName(deceased.name())
                .setDeceasedNumber(deceased.number())
                .setDeceasedIdentityNumber(deceased.identityNumber())
                .setClaimantName(claimant.name())
                .setClaimType(entity.getClaimType())
                .setCoveragePlanId(entity.getCoveragePlanId())
                .setCoveragePlanName(coveragePlanName(entity.getCoveragePlanId()))
                .setCoverageEventDate(entity.getCoverageEventDate())
                .setDeceasedType(entity.getDeceasedType())
                .setDeceasedPartnerId(entity.getDeceasedPartnerId())
                .setDateOfDeath(entity.getDateOfDeath())
                .setClaimDate(entity.getClaimDate())
                .setBurialDate(entity.getBurialDate())
                .setCauseOfDeath(entity.getCauseOfDeath())
                .setDeathCertificateNo(entity.getDeathCertificateNo())
                .setClaimantPartnerId(entity.getClaimantPartnerId())
                .setClaimAmountCents(entity.getClaimAmountCents())
                .setApprovedAmountCents(resolveApprovedAmount(entity))
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
                .setApprovalRequestId(entity.getApprovalRequestId())
                .setApprovedBy(entity.getApprovedBy())
                .setApprovedAt(entity.getApprovedAt())
                .setPaymentRequestId(entity.getPaymentRequestId())
                .setTombstoneOrderId(entity.getTombstoneOrderId())
                .setSettlementMethod(entity.getSettlementMethod())
                .setSettlementReference(entity.getSettlementReference())
                .setSettledAt(entity.getSettledAt())
                .setLinkedClaims(linkedClaims)
                .setPayoutMethod(entity.getPayoutMethod())
                .setBankName(entity.getBankName())
                .setAccountHolderName(entity.getAccountHolderName())
                .setAccountNumber(entity.getAccountNumber())
                .setBranchCode(entity.getBranchCode())
                .setAccountType(entity.getAccountType());

    }
    private String coveragePlanName(String planId) {
        if (planId == null || planId.isBlank()) return null;
        return membershipPlanRepository.findById(planId)
                .map(MembershipPlanEntity::getName)
                .orElse(planId);
    }

    private MembershipReference membershipReference(String membershipId) {
        if (!StringUtils.hasText(membershipId)) {
            return new MembershipReference("", PartnerReference.empty());
        }
        try {
            return jdbcTemplate.query(
                    """
                    SELECT m.membership_no,
                           COALESCE(p.partner_no, '') AS partner_no,
                           COALESCE(p.identity_number, '') AS identity_number,
                           TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) AS partner_name
                      FROM membership m
                      LEFT JOIN partner_view p ON p.partner_id = m.member_id
                     WHERE m.id = ?
                    """,
                    resultSet -> resultSet.next()
                            ? new MembershipReference(
                                    resultSet.getString("membership_no"),
                                    new PartnerReference(
                                            resultSet.getString("partner_no"),
                                            resultSet.getString("identity_number"),
                                            resultSet.getString("partner_name")
                                    )
                              )
                            : new MembershipReference(membershipId, PartnerReference.empty()),
                    membershipId
            );
        } catch (Exception ignored) {
            return new MembershipReference(membershipId, PartnerReference.empty());
        }
    }

    private PartnerReference partnerReference(String partnerId) {
        if (!StringUtils.hasText(partnerId)) return PartnerReference.empty();
        try {
            return jdbcTemplate.query(
                    """
                    SELECT COALESCE(partner_no, '') AS partner_no,
                           COALESCE(identity_number, '') AS identity_number,
                           TRIM(CONCAT_WS(' ', NULLIF(name2,''), NULLIF(name3,''), NULLIF(name1,''))) AS partner_name
                      FROM partner_view
                     WHERE partner_id = ?
                    """,
                    resultSet -> resultSet.next()
                            ? new PartnerReference(
                                    resultSet.getString("partner_no"),
                                    resultSet.getString("identity_number"),
                                    resultSet.getString("partner_name")
                              )
                            : PartnerReference.empty(),
                    partnerId
            );
        } catch (Exception ignored) {
            return PartnerReference.empty();
        }
    }

    private record MembershipReference(String membershipNo, PartnerReference member) { }

    private record PartnerReference(String number, String identityNumber, String name) {
        private static PartnerReference empty() {
            return new PartnerReference("", "", "");
        }
    }

}
