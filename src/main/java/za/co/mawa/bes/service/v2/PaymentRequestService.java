package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.v2.payment.*;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestStatusHistoryRepository;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service(value = "paymentRequestServiceV2")
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentRequestStatusHistoryRepository statusHistoryRepository;
    private final PaymentRequestFnbPaymentQueueService fnbPaymentQueueService;
    private final NumberRangeService numberRangeService;
    private final MembershipClaimService membershipClaimService;
    private final PaymentAccountConfigurationService paymentAccountConfigurationService;
    private final JdbcTemplate jdbcTemplate;
    private final za.co.mawa.bes.repository.AttachmentRepository attachmentRepository;

    @Autowired
    SettingService settingService;

    public PaymentRequestService(
            PaymentRequestRepository paymentRequestRepository,
            PaymentRequestStatusHistoryRepository statusHistoryRepository,
            PaymentRequestFnbPaymentQueueService fnbPaymentQueueService,
            NumberRangeService numberRangeService,
            MembershipClaimService membershipClaimService,
            PaymentAccountConfigurationService paymentAccountConfigurationService,
            JdbcTemplate jdbcTemplate,
            za.co.mawa.bes.repository.AttachmentRepository attachmentRepository
    ) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.fnbPaymentQueueService = fnbPaymentQueueService;
        this.numberRangeService = numberRangeService;
        this.membershipClaimService = membershipClaimService;
        this.paymentAccountConfigurationService = paymentAccountConfigurationService;
        this.jdbcTemplate = jdbcTemplate;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public PaymentRequestResponse create(PaymentRequestCreateRequest request, String currentUser) {
        applyTypeRules(request);
        validateCreateRequest(request);

        String idempotencyKey = request.getIdempotencyKey() == null ? null : request.getIdempotencyKey().trim();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<PaymentRequestEntity> existing = paymentRequestRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setRequestNo(generateRequestNo());
        entity.setRequestType(request.getRequestType());
        entity.setSourceType(request.getSourceType());
        entity.setSourceId(request.getSourceId());
        entity.setPayeePartnerId(request.getPayeePartnerId());
        entity.setPayeeName(request.getPayeeName());
        entity.setAmount(request.getAmount());
        entity.setCurrency(defaultCurrency(request.getCurrency()));
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setBankName(request.getBankName());
        entity.setAccountHolder(request.getAccountHolder());
        entity.setAccountNumber(request.getAccountNumber());
        entity.setBranchCode(request.getBranchCode());
        entity.setAccountType(request.getAccountType());
        entity.setInvoiceNo(request.getInvoiceNo());
        entity.setExternalReference(request.getExternalReference());
        entity.setPaymentReason(request.getPaymentReason());
        entity.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        entity.setNotes(request.getNotes());
        entity.setRequestedPaymentDate(request.getRequestedPaymentDate());
        entity.setStatus(PaymentRequestStatus.DRAFT);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), null, PaymentRequestStatus.DRAFT, "Payment request created", currentUser);
        return toResponse(saved);
    }

    @Transactional
    public PaymentRequestResponse createOrReuseApprovedClaimPayout(
            MembershipClaimResponse claim,
            ApprovalRequestEntity approvalRequest,
            String actionBy
    ) {
        if (claim == null || claim.getId() == null) {
            throw new IllegalArgumentException("Approved claim is required");
        }
        long payoutAmountCents = claim.getApprovedAmountCents() != null && claim.getApprovedAmountCents() > 0
                ? claim.getApprovedAmountCents()
                : (claim.getClaimAmountCents() == null ? 0L : claim.getClaimAmountCents());
        if (payoutAmountCents <= 0) {
            throw new IllegalArgumentException("Approved CASH claim amount must be greater than zero");
        }
        if (claim.getPayoutMethod() == null) {
            throw new IllegalArgumentException("Payout method is required before approving a CASH claim");
        }

        String idempotencyKey = claimPayoutIdempotencyKey(claim.getId());
        PaymentRequestEntity entity = paymentRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> paymentRequestRepository
                        .findFirstBySourceTypeAndSourceIdAndRequestTypeOrderByCreatedAtAsc(
                                PaymentRequestSourceType.MEMBERSHIP_CLAIM,
                                claim.getId(),
                                PaymentRequestType.CLAIM_PAYOUT
                        )
                        .orElse(null));

        boolean created = entity == null;
        if (created) {
            entity = new PaymentRequestEntity();
            entity.setRequestNo(generateRequestNo());
            entity.setRequestType(PaymentRequestType.CLAIM_PAYOUT);
            entity.setSourceType(PaymentRequestSourceType.MEMBERSHIP_CLAIM);
            entity.setSourceId(claim.getId());
            entity.setPayeePartnerId(claim.getClaimantPartnerId());
            entity.setPayeeName(firstNonBlank(claim.getAccountHolderName(), claim.getClaimantName()));
            entity.setAmount(BigDecimal.valueOf(payoutAmountCents, 2));
            entity.setCurrency("ZAR");
            entity.setPaymentMethod(claim.getPayoutMethod());
            entity.setBankName(claim.getBankName());
            entity.setAccountHolder(claim.getAccountHolderName());
            entity.setAccountNumber(claim.getAccountNumber());
            entity.setBranchCode(claim.getBranchCode());
            entity.setAccountType(claim.getAccountType() == null ? null : claim.getAccountType().name());
            entity.setExternalReference("CLAIM-" + claim.getClaimNo());
            entity.setPaymentReason("CASH-CLAIM-PAYOUT");
            entity.setPaymentPurpose("CASH_CLAIM_DISBURSEMENT");
            entity.setRequestedPaymentDate(LocalDate.now());
            entity.setCreatedBy(systemActor(actionBy));
        }

        // Immutable snapshot used by the bank. For an existing DRAFT request created by the
        // previous implementation, fill missing values before inheriting claim approval.
        if (entity.getStatus() == null || entity.getStatus() == PaymentRequestStatus.DRAFT
                || entity.getStatus() == PaymentRequestStatus.PENDING_APPROVAL) {
            entity.setPayeePartnerId(claim.getClaimantPartnerId());
            entity.setPayeeName(firstNonBlank(claim.getAccountHolderName(), claim.getClaimantName()));
            entity.setAmount(BigDecimal.valueOf(payoutAmountCents, 2));
            entity.setPaymentMethod(claim.getPayoutMethod());
            entity.setBankName(claim.getBankName());
            entity.setAccountHolder(claim.getAccountHolderName());
            entity.setAccountNumber(claim.getAccountNumber());
            entity.setBranchCode(claim.getBranchCode());
            entity.setAccountType(claim.getAccountType() == null ? null : claim.getAccountType().name());
        }

        entity.setIdempotencyKey(idempotencyKey);
        entity.setApprovalRequestId(approvalRequest == null ? claim.getApprovalRequestId() : approvalRequest.getId());
        entity.setApprovalSource("CLAIM_APPROVAL");
        entity.setApprovalReference(approvalRequest == null ? claim.getApprovalRequestId() : approvalRequest.getId());
        entity.setApprovalInherited(true);
        entity.setPaymentPurpose("CASH_CLAIM_DISBURSEMENT");
        entity.setUpdatedBy(systemActor(actionBy));

        validateEntity(entity);

        PaymentRequestStatus oldStatus = entity.getStatus();
        if (oldStatus == null || oldStatus == PaymentRequestStatus.DRAFT
                || oldStatus == PaymentRequestStatus.PENDING_APPROVAL
                || oldStatus == PaymentRequestStatus.REJECTED) {
            entity.setStatus(PaymentRequestStatus.APPROVED);
            entity.setApprovedBy(systemActor(actionBy));
            entity.setApprovedAt(new Date());
        }

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        if (created) {
            saveHistory(saved.getId(), null, PaymentRequestStatus.APPROVED,
                    "Payment request created and authorised by approved CASH claim", systemActor(actionBy));
        } else if (oldStatus != saved.getStatus()) {
            saveHistory(saved.getId(), oldStatus, saved.getStatus(),
                    "Payment approval inherited from approved CASH claim", systemActor(actionBy));
        }

        if (saved.getStatus() == PaymentRequestStatus.APPROVED) {
            fnbPaymentQueueService.queueAfterApproval(saved.getId(), saved.getRequestNo(), systemActor(actionBy));
            saved = paymentRequestRepository.findById(saved.getId()).orElse(saved);
        }
        return toResponse(saved);
    }

    public List<PaymentRequestResponse> getAll() {
        return paymentRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PaymentRequestResponse getById(String id) {
        return toResponse(findById(id));
    }

    public List<PaymentRequestResponse> getByStatus(PaymentRequestStatus status) {
        return paymentRequestRepository.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toResponse).toList();
    }

    public List<PaymentRequestResponse> getByType(PaymentRequestType type) {
        return paymentRequestRepository.findByRequestTypeOrderByCreatedAtDesc(type).stream().map(this::toResponse).toList();
    }

    public List<PaymentRequestResponse> getByPayeePartner(String partnerId) {
        return paymentRequestRepository.findByPayeePartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentRequestResponse update(String id, PaymentRequestUpdateRequest request, String currentUser) {
        PaymentRequestEntity entity = findById(id);

        if (entity.getStatus() != PaymentRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT payment requests can be updated.");
        }

        if (request.getPayeePartnerId() != null) entity.setPayeePartnerId(request.getPayeePartnerId());
        if (request.getPayeeName() != null) entity.setPayeeName(request.getPayeeName());
        if (request.getAmount() != null) entity.setAmount(request.getAmount());
        if (request.getCurrency() != null) entity.setCurrency(defaultCurrency(request.getCurrency()));
        if (request.getPaymentMethod() != null) entity.setPaymentMethod(request.getPaymentMethod());

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

        validateEntity(entity);
        entity.setUpdatedBy(currentUser);
        return toResponse(paymentRequestRepository.save(entity));
    }

    @Transactional
    public PaymentRequestResponse submit(String id, String currentUser) {
        PaymentRequestEntity entity = findById(id);

        if (entity.getStatus() != PaymentRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT payment requests can be submitted.");
        }

        validateEntity(entity);
        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.PENDING_APPROVAL);
        entity.setUpdatedBy(currentUser);

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), oldStatus, PaymentRequestStatus.PENDING_APPROVAL, "Payment request submitted for approval", currentUser);
        return toResponse(saved);
    }

    @Transactional
    public PaymentRequestResponse updateStatus(String id, PaymentRequestStatusUpdateRequest request, String currentUser) {
        PaymentRequestEntity entity = findById(id);
        PaymentRequestStatus oldStatus = entity.getStatus();
        PaymentRequestStatus newStatus = request.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        entity.setStatus(newStatus);
        entity.setUpdatedBy(currentUser);

        if (request.getApprovalRequestId() != null) {
            entity.setApprovalRequestId(request.getApprovalRequestId());
        }

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), oldStatus, newStatus, request.getComment(), currentUser);

        if (newStatus == PaymentRequestStatus.APPROVED) {
            fnbPaymentQueueService.queueAfterApproval(saved.getId(), saved.getRequestNo(), currentUser);
            saved = paymentRequestRepository.findById(saved.getId()).orElse(saved);
        }

        return toResponse(saved);
    }

    @Transactional
    public PaymentRequestResponse cancel(String id, String comment, String currentUser) {
        PaymentRequestEntity entity = findById(id);

        if (entity.getStatus() == PaymentRequestStatus.PAID) {
            throw new IllegalStateException("Paid payment requests cannot be cancelled.");
        }

        if (entity.getStatus() == PaymentRequestStatus.CANCELLED) {
            throw new IllegalStateException("Payment request is already cancelled.");
        }

        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.CANCELLED);
        entity.setUpdatedBy(currentUser);

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), oldStatus, PaymentRequestStatus.CANCELLED,
                comment == null || comment.isBlank() ? "Payment request cancelled" : comment,
                currentUser);
        return toResponse(saved);
    }

    @Transactional
    public PaymentRequestResponse markPaid(String id, MarkPaymentRequestPaidRequest request, String currentUser) {
        PaymentRequestEntity proofEntity = findById(id);
        if (proofEntity.getPaymentMethod() == PaymentMethod.MANUAL) {
            if (request.getProofAttachmentId() == null || request.getProofAttachmentId().isBlank()) throw new IllegalArgumentException("Proof of payment attachment is required for manual payments");
            za.co.mawa.bes.entity.AttachmentEntity proof = attachmentRepository.findById(request.getProofAttachmentId()).orElseThrow(() -> new IllegalArgumentException("Proof attachment not found"));
            if (!id.equals(proof.getObjectId())) throw new IllegalArgumentException("Proof attachment must belong to this payment request");
            proofEntity.setManualProofAttachmentId(proof.getId());
            paymentRequestRepository.save(proofEntity);
        }
        PaymentRequestEntity entity = findById(id);

        if (entity.getStatus() != PaymentRequestStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED payment requests can be marked as PAID.");
        }

        if (request.getPaidReference() == null || request.getPaidReference().isBlank()) {
            throw new IllegalArgumentException("Paid reference is required.");
        }

        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.PAID);
        entity.setPaidDate(request.getPaidDate() == null ? LocalDate.now() : request.getPaidDate());
        entity.setPaidReference(request.getPaidReference());
        entity.setPaidBy(currentUser);
        entity.setUpdatedBy(currentUser);

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), oldStatus, PaymentRequestStatus.PAID,
                request.getComment() == null || request.getComment().isBlank() ? "Payment request marked as paid" : request.getComment(),
                currentUser);
        if (saved.getSourceType() == PaymentRequestSourceType.MEMBERSHIP_CLAIM && saved.getSourceId() != null) {
            membershipClaimService.markPaymentPaid(saved.getSourceId(), systemActor(currentUser));
        }
        return toResponse(saved);
    }

    public List<PaymentRequestStatusHistoryEntity> getHistory(String id) {
        findById(id);
        return statusHistoryRepository.findByPaymentRequestIdOrderByChangedAtAsc(id);
    }

    public PaymentRequestEntity findById(String id) {
        return paymentRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found: " + id));
    }

    @Transactional
    public void markApproved(String paymentRequestId, String approvedBy) {
        PaymentRequestEntity entity = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + paymentRequestId));

        if (entity.getStatus() == PaymentRequestStatus.APPROVED) {
            return;
        }

        if (entity.getStatus() == PaymentRequestStatus.REJECTED ||
                entity.getStatus() == PaymentRequestStatus.CANCELLED ||
                entity.getStatus() == PaymentRequestStatus.PAID ||
                entity.getStatus() == PaymentRequestStatus.PROCESSED) {
            throw new RuntimeException("Payment request cannot be approved from status: " + entity.getStatus());
        }

        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.APPROVED);
        entity.setApprovedBy(approvedBy);
        entity.setApprovedAt(new Date());
        entity.setUpdatedBy(approvedBy);

        paymentRequestRepository.save(entity);
        saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.APPROVED,
                "Payment request approved", approvedBy);
    }

    @Transactional
    public void linkApproval(PaymentRequestEntity entity) {
        paymentRequestRepository.save(entity);
    }

    @Transactional
    public void markQueuedForPayment(String paymentRequestId, String updatedBy) {
        PaymentRequestEntity entity = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + paymentRequestId));

        if (entity.getStatus() == PaymentRequestStatus.QUEUED_FOR_PAYMENT) {
            return;
        }

        if (entity.getStatus() != PaymentRequestStatus.APPROVED) {
            throw new RuntimeException("Payment request must be APPROVED before queueing payment");
        }

        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.QUEUED_FOR_PAYMENT);
        entity.setUpdatedBy(updatedBy);

        paymentRequestRepository.save(entity);
        saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.QUEUED_FOR_PAYMENT,
                "Payment request queued for FNB EFT payment", updatedBy);
    }

    public String getFnbInstructionId(String paymentRequestIdOrRequestNo) {
        return findByIdOrRequestNo(paymentRequestIdOrRequestNo).getFnbInstructionId();
    }

    /**
     * Persists the FNB instruction identifier in an independent transaction immediately
     * after FNB accepts the initiation request. This makes queue retries idempotent: if
     * a later local update fails, the next attempt reuses the stored instruction ID and
     * does not initiate a second payment at FNB.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFnbInstruction(String paymentRequestIdOrRequestNo, String instructionId, String updatedBy) {
        if (instructionId == null || instructionId.isBlank()) {
            throw new IllegalArgumentException("FNB instruction ID is required");
        }

        PaymentRequestEntity entity = findByIdOrRequestNo(paymentRequestIdOrRequestNo);
        String existingInstructionId = entity.getFnbInstructionId();
        if (existingInstructionId != null && !existingInstructionId.isBlank()) {
            if (!existingInstructionId.equals(instructionId)) {
                throw new IllegalStateException(
                        "Payment request already has a different FNB instruction ID: " + entity.getRequestNo()
                );
            }
            return;
        }

        entity.setFnbInstructionId(instructionId);
        // Keep the existing field populated for backward compatibility with older clients.
        if (entity.getPaidReference() == null || entity.getPaidReference().isBlank()) {
            entity.setPaidReference(instructionId);
        }
        entity.setUpdatedBy(systemActor(updatedBy));
        paymentRequestRepository.saveAndFlush(entity);
    }

    @Transactional
    public void markSentToBank(String paymentRequestIdOrRequestNo, String instructionId, String updatedBy) {
        PaymentRequestEntity entity = findByIdOrRequestNo(paymentRequestIdOrRequestNo);
        String effectiveInstructionId = firstNonBlank(instructionId, entity.getFnbInstructionId());

        if (effectiveInstructionId == null || effectiveInstructionId.isBlank()) {
            throw new IllegalArgumentException("FNB instruction ID is required");
        }

        if (entity.getStatus() == PaymentRequestStatus.PROCESSED &&
                effectiveInstructionId.equals(entity.getFnbInstructionId())) {
            return;
        }

        if (entity.getStatus() != PaymentRequestStatus.QUEUED_FOR_PAYMENT &&
                entity.getStatus() != PaymentRequestStatus.APPROVED &&
                entity.getStatus() != PaymentRequestStatus.PROCESSED) {
            throw new RuntimeException("Payment request cannot be marked as sent to bank from status: " + entity.getStatus());
        }

        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.PROCESSED);
        entity.setFnbInstructionId(effectiveInstructionId);
        if (entity.getPaidReference() == null || entity.getPaidReference().isBlank()) {
            entity.setPaidReference(effectiveInstructionId);
        }
        entity.setUpdatedBy(systemActor(updatedBy));

        paymentRequestRepository.save(entity);
        if (oldStatus != PaymentRequestStatus.PROCESSED) {
            saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.PROCESSED,
                    "Payment request sent to FNB. Instruction ID: " + effectiveInstructionId, systemActor(updatedBy));
        }
        updateLinkedClaimProcessing(entity, systemActor(updatedBy));
    }

    @Transactional
    public void markBankPaymentPending(String paymentRequestIdOrRequestNo, String providerStatus, String updatedBy) {
        PaymentRequestEntity entity = findByIdOrRequestNo(paymentRequestIdOrRequestNo);
        if (entity.getStatus() == PaymentRequestStatus.PAID || entity.getStatus() == PaymentRequestStatus.FAILED) return;
        if (entity.getStatus() != PaymentRequestStatus.PROCESSED) {
            PaymentRequestStatus oldStatus = entity.getStatus();
            entity.setStatus(PaymentRequestStatus.PROCESSED);
            entity.setUpdatedBy(systemActor(updatedBy));
            paymentRequestRepository.save(entity);
            saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.PROCESSED,
                    "FNB payment is processing: " + providerStatus, systemActor(updatedBy));
        }
        updateLinkedClaimProcessing(entity, systemActor(updatedBy));
    }

    @Transactional
    public void markBankPaymentPaid(String paymentRequestIdOrRequestNo, String providerStatus, String updatedBy) {
        PaymentRequestEntity entity = findByIdOrRequestNo(paymentRequestIdOrRequestNo);
        if (entity.getStatus() == PaymentRequestStatus.PAID) return;
        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.PAID);
        entity.setPaidDate(LocalDate.now());
        entity.setPaidBy(systemActor(updatedBy));
        entity.setUpdatedBy(systemActor(updatedBy));
        paymentRequestRepository.save(entity);
        saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.PAID,
                "FNB confirmed payment: " + providerStatus, systemActor(updatedBy));
        if (entity.getSourceType() == PaymentRequestSourceType.MEMBERSHIP_CLAIM && entity.getSourceId() != null) {
            membershipClaimService.markPaymentPaid(entity.getSourceId(), systemActor(updatedBy));
        }
    }

    @Transactional
    public void markBankPaymentFailed(String paymentRequestIdOrRequestNo, String providerStatus, String reason, String updatedBy) {
        PaymentRequestEntity entity = findByIdOrRequestNo(paymentRequestIdOrRequestNo);
        if (entity.getStatus() == PaymentRequestStatus.PAID) return;
        PaymentRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(PaymentRequestStatus.FAILED);
        entity.setUpdatedBy(systemActor(updatedBy));
        paymentRequestRepository.save(entity);
        if (oldStatus != PaymentRequestStatus.FAILED) {
            saveHistory(entity.getId(), oldStatus, PaymentRequestStatus.FAILED,
                    "FNB payment failed [" + providerStatus + "]: " + reason, systemActor(updatedBy));
        }
        if (entity.getSourceType() == PaymentRequestSourceType.MEMBERSHIP_CLAIM && entity.getSourceId() != null) {
            membershipClaimService.markPaymentFailed(entity.getSourceId(), reason, systemActor(updatedBy));
        }
    }

    private void updateLinkedClaimProcessing(PaymentRequestEntity entity, String updatedBy) {
        if (entity.getSourceType() == PaymentRequestSourceType.MEMBERSHIP_CLAIM && entity.getSourceId() != null) {
            membershipClaimService.markPaymentProcessing(entity.getSourceId(), updatedBy);
        }
    }

    private PaymentRequestEntity findByIdOrRequestNo(String paymentRequestIdOrRequestNo) {
        Optional<PaymentRequestEntity> byId = paymentRequestRepository.findById(paymentRequestIdOrRequestNo);
        if (byId.isPresent()) {
            return byId.get();
        }

        return paymentRequestRepository.findByRequestNo(paymentRequestIdOrRequestNo)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + paymentRequestIdOrRequestNo));
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String claimPayoutIdempotencyKey(String claimId) {
        return "MEMBERSHIP_CLAIM:" + claimId + ":CASH_CLAIM_DISBURSEMENT";
    }

    private String systemActor(String updatedBy) {
        return updatedBy == null || updatedBy.isBlank() ? "SYSTEM" : updatedBy;
    }

    private void validateCreateRequest(PaymentRequestCreateRequest request) {
        if (request.getRequestType() == null) throw new IllegalArgumentException("Request type is required.");
        if (request.getPayeeName() == null || request.getPayeeName().isBlank()) throw new IllegalArgumentException("Payee name is required.");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (request.getPaymentMethod() == null) throw new IllegalArgumentException("Payment method is required.");
        if (request.getPaymentMethod() == PaymentMethod.EFT) {
            validateBankingDetails(request.getBankName(), request.getAccountHolder(), request.getAccountNumber(), request.getBranchCode());
        }
    }

    private void validateEntity(PaymentRequestEntity entity) {
        if (entity.getRequestType() == null) throw new IllegalArgumentException("Request type is required.");
        if (entity.getPayeeName() == null || entity.getPayeeName().isBlank()) throw new IllegalArgumentException("Payee name is required.");
        if (entity.getAmount() == null || entity.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (entity.getPaymentMethod() == null) throw new IllegalArgumentException("Payment method is required.");
        if (entity.getPaymentMethod() == PaymentMethod.EFT) {
            validateBankingDetails(entity.getBankName(), entity.getAccountHolder(), entity.getAccountNumber(), entity.getBranchCode());
        }
    }

    private void validateBankingDetails(String bankName, String accountHolder, String accountNumber, String branchCode) {
        if (bankName == null || bankName.isBlank()) throw new IllegalArgumentException("Bank name is required for EFT payment.");
        if (accountHolder == null || accountHolder.isBlank()) throw new IllegalArgumentException("Account holder is required for EFT payment.");
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required for EFT payment.");
        if (branchCode == null || branchCode.isBlank()) throw new IllegalArgumentException("Branch code is required for EFT payment.");
    }

    private void validateStatusTransition(PaymentRequestStatus oldStatus, PaymentRequestStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("New status is required.");
        if (oldStatus == PaymentRequestStatus.PAID) throw new IllegalStateException("Paid payment requests cannot be changed.");
        if (oldStatus == PaymentRequestStatus.CANCELLED) throw new IllegalStateException("Cancelled payment requests cannot be changed.");

        boolean valid = oldStatus == PaymentRequestStatus.PENDING_APPROVAL &&
                (newStatus == PaymentRequestStatus.APPROVED || newStatus == PaymentRequestStatus.REJECTED)
                || oldStatus == PaymentRequestStatus.REJECTED && newStatus == PaymentRequestStatus.DRAFT
                || oldStatus == PaymentRequestStatus.APPROVED && newStatus == PaymentRequestStatus.PAID;

        if (!valid) throw new IllegalStateException("Invalid status transition from " + oldStatus + " to " + newStatus);
    }

    private void saveHistory(String paymentRequestId, PaymentRequestStatus oldStatus, PaymentRequestStatus newStatus, String comment, String currentUser) {
        PaymentRequestStatusHistoryEntity history = new PaymentRequestStatusHistoryEntity();
        history.setPaymentRequestId(paymentRequestId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setComment(comment);
        history.setChangedBy(currentUser);
        statusHistoryRepository.save(history);
    }

    private String generateRequestNo() {
        try {
            return numberRangeService.generateNumber(TransactionType.PAYMENT_REQUEST);
        } catch (NumberRangeObjectNotFound e) {
            throw new IllegalStateException("Payment Request number range is not configured for object: "
                    + TransactionType.PAYMENT_REQUEST, e);
        }
    }

    private String defaultCurrency(String currency) {
        return currency == null || currency.isBlank() ? "ZAR" : currency;
    }

    private void applyTypeRules(PaymentRequestCreateRequest request) {
        if (request.getRequestType() == null) throw new IllegalArgumentException("Payment request type must be selected first");
        if (request.getRequestType() == PaymentRequestType.SUPPLIER_INVOICE) {
            if (request.getPayeePartnerId() == null || request.getPayeePartnerId().isBlank()) throw new IllegalArgumentException("Supplier is required");
            List<java.util.Map<String,Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id partner_id, CONCAT(COALESCE(p.first_name,''),' ',COALESCE(p.last_name,'')) payee_name,
                       b.bank_name,b.account_holder,b.account_number,b.branch_code,b.account_type
                  FROM partner p JOIN partner_role pr ON pr.partner=p.id AND pr.role='SUPPLIER'
                  JOIN partner_bank_account b ON b.partner=p.id
                 WHERE p.id=? ORDER BY b.id LIMIT 1
                """, request.getPayeePartnerId());
            if (rows.isEmpty()) throw new IllegalArgumentException("Selected recipient is not a supplier or has no banking details");
            applyCreditor(request, rows.get(0));
        } else if (request.getRequestType() == PaymentRequestType.PETTY_CASH_REPLENISHMENT) {
            var creditor = paymentAccountConfigurationService.activeCreditor("PETTY_CASH_CREDITOR");
            if (creditor.isPresent()) applyCreditor(request, creditor.get()); else request.setPaymentMethod(PaymentMethod.MANUAL);
        }
        var debtor = paymentAccountConfigurationService.activeDebtor(request.getRequestType().name());
        if (debtor.isEmpty()) { request.setPaymentMethod(PaymentMethod.MANUAL); return; }
        String integration = java.util.Objects.toString(debtor.get().get("bank_integration"), "");
        boolean fnb = "FNB".equalsIgnoreCase(integration) && isFnbEnabled();
        request.setPaymentMethod(fnb ? PaymentMethod.EFT : PaymentMethod.MANUAL);
    }

    private void applyCreditor(PaymentRequestCreateRequest request, java.util.Map<String,Object> account) {
        request.setBankName(java.util.Objects.toString(account.get("bank_name"), null));
        request.setAccountHolder(java.util.Objects.toString(account.get("account_holder"), request.getPayeeName()));
        request.setAccountNumber(java.util.Objects.toString(account.get("account_number"), null));
        request.setBranchCode(java.util.Objects.toString(account.get("branch_code"), null));
        request.setAccountType(java.util.Objects.toString(account.get("account_type"), null));
    }

    private boolean isFnbEnabled() {
        String value = settingService.getSetting("ENABLED", "FNB-API");
        return value != null && java.util.Set.of("1","true","Y","yes").contains(value.trim());
    }

    private BankAccountCreateDto getCashBankAccount() {
        BankAccountCreateDto bankAccountDto = new BankAccountCreateDto();
        bankAccountDto.setAccountHolder(settingService.getSetting("ACCOUNT-HOLDER", "CASH-BANK-ACCOUNT"));
        bankAccountDto.setBankName(settingService.getSetting("BANK-NAME", "CASH-BANK-ACCOUNT"));
        bankAccountDto.setBranchCode(settingService.getSetting("BRANCH-CODE", "CASH-BANK-ACCOUNT"));
        bankAccountDto.setAccountNumber(settingService.getSetting("ACCOUNT-NUMBER", "CASH-BANK-ACCOUNT"));
        bankAccountDto.setAccountType(settingService.getSetting("ACCOUNT-TYPE", "CASH-BANK-ACCOUNT"));
        return bankAccountDto;
    }

    private PaymentRequestResponse toResponse(PaymentRequestEntity entity) {
        return new PaymentRequestResponse()
                .setId(entity.getId())
                .setRequestNo(entity.getRequestNo())
                .setRequestType(entity.getRequestType())
                .setSourceType(entity.getSourceType())
                .setSourceId(entity.getSourceId())
                .setPayeePartnerId(entity.getPayeePartnerId())
                .setPayeeName(entity.getPayeeName())
                .setAmount(entity.getAmount())
                .setCurrency(entity.getCurrency())
                .setPaymentMethod(entity.getPaymentMethod())
                .setBankName(entity.getBankName())
                .setAccountHolder(entity.getAccountHolder())
                .setAccountNumber(entity.getAccountNumber())
                .setBranchCode(entity.getBranchCode())
                .setAccountType(entity.getAccountType())
                .setInvoiceNo(entity.getInvoiceNo())
                .setExternalReference(entity.getExternalReference())
                .setPaymentReason(entity.getPaymentReason())
                .setNotes(entity.getNotes())
                .setRequestedPaymentDate(entity.getRequestedPaymentDate())
                .setStatus(entity.getStatus())
                .setApprovalRequestId(entity.getApprovalRequestId())
                .setApprovalSource(entity.getApprovalSource())
                .setApprovalReference(entity.getApprovalReference())
                .setApprovalInherited(entity.isApprovalInherited())
                .setPaymentPurpose(entity.getPaymentPurpose())
                .setIdempotencyKey(entity.getIdempotencyKey())
                .setPaidDate(entity.getPaidDate())
                .setPaidReference(entity.getPaidReference())
                .setFnbInstructionId(entity.getFnbInstructionId())
                .setPaidBy(entity.getPaidBy())
                .setCreatedAt(entity.getCreatedAt())
                .setCreatedBy(entity.getCreatedBy())
                .setUpdatedAt(entity.getUpdatedAt())
                .setUpdatedBy(entity.getUpdatedBy());
    }
}
