package za.co.mawa.bes.service.v2.tombstone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestCreateRequest;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneDtos;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.entity.v2.tombstone.*;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.repository.v2.tombstone.*;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.v2.NumberAllocationService;
import za.co.mawa.bes.service.v2.PaymentRequestService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TombstoneManagementService {

    private static final Set<String> FUNDING_METHODS = Set.of("CASH", "LAYBY", "FUNERAL_COVER", "COMBINATION");
    private static final Set<String> FUNDING_TYPES = Set.of("CASH", "LAYBY", "FUNERAL_COVER");
    private static final Set<String> PRODUCTION_STATUSES = Set.of(
            "MATERIAL_ORDERED", "MATERIAL_RECEIVED", "CUTTING", "ENGRAVING", "ASSEMBLY",
            "QUALITY_CHECK", "READY_FOR_INSTALLATION", "CANCELLED"
    );
    private static final Set<String> INSTALLATION_OPERATIONAL_STATUSES = Set.of(
            "READY_TO_SCHEDULE", "SCHEDULED", "TEAM_DISPATCHED", "ON_SITE",
            "REWORK_REQUIRED", "CANCELLED"
    );
    private static final List<String[]> DEFAULT_CHECKLIST = List.of(
            new String[]{"CORRECT_CEMETERY", "Correct cemetery and grave confirmed"},
            new String[]{"CORRECT_TOMBSTONE", "Correct tombstone and approved design confirmed"},
            new String[]{"FOUNDATION_COMPLETE", "Foundation completed and stable"},
            new String[]{"LEVEL_AND_SECURE", "Tombstone is level and secure"},
            new String[]{"INSCRIPTION_VERIFIED", "Inscription verified against approved design"},
            new String[]{"SITE_CLEANED", "Installation area cleaned"},
            new String[]{"BEFORE_PHOTOS", "Before photographs captured"},
            new String[]{"AFTER_PHOTOS", "After photographs captured"}
    );

    private final TombstoneOrderRepository orderRepository;
    private final TombstoneOrderItemRepository itemRepository;
    private final TombstoneFundingAllocationRepository fundingRepository;
    private final TombstoneLaybyAgreementRepository laybyRepository;
    private final TombstoneLaybyInstallmentRepository installmentRepository;
    private final TombstoneSiteAssessmentRepository assessmentRepository;
    private final TombstoneOrderAmendmentRepository amendmentRepository;
    private final TombstoneDesignRepository designRepository;
    private final TombstoneProductionJobRepository productionRepository;
    private final TombstoneInstallationRepository installationRepository;
    private final TombstoneInstallationTeamRepository teamRepository;
    private final TombstoneInstallationMaterialRepository materialRepository;
    private final TombstoneInstallationChecklistRepository checklistRepository;
    private final TombstoneStatusHistoryRepository historyRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ReceiptAllocationRepository receiptAllocationRepository;
    private final MembershipClaimRepository claimRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final PaymentRequestService paymentRequestService;
    private final NumberAllocationService numberAllocationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TombstoneDtos.OrderResponse createOrder(TombstoneDtos.CreateOrderRequest request, String actor) {
        require(request, "Order request is required");
        requireText(request.getCustomerPartnerId(), "customerPartnerId");
        requireText(request.getDeceasedName(), "deceasedName");
        String fundingMethod = normalizeRequired(request.getFundingMethod(), "fundingMethod");
        if (!FUNDING_METHODS.contains(fundingMethod)) {
            throw new IllegalArgumentException("fundingMethod must be CASH, LAYBY, FUNERAL_COVER or COMBINATION");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one tombstone order item is required");
        }

        TombstoneOrderEntity order = TombstoneOrderEntity.builder()
                .orderNo(formatNumber("TSO", numberAllocationService.allocateNumber("TOMBSTONE_ORDER")))
                .customerPartnerId(request.getCustomerPartnerId().trim())
                .membershipId(trimToNull(request.getMembershipId()))
                .deceasedPartnerId(trimToNull(request.getDeceasedPartnerId()))
                .deceasedName(request.getDeceasedName().trim())
                .funeralServiceId(trimToNull(request.getFuneralServiceId()))
                .cemeteryName(trimToNull(request.getCemeteryName()))
                .cemeteryArea(trimToNull(request.getCemeteryArea()))
                .graveNumber(trimToNull(request.getGraveNumber()))
                .graveLatitude(request.getGraveLatitude())
                .graveLongitude(request.getGraveLongitude())
                .salesArea(trimToNull(request.getSalesArea()))
                .workcenterId(trimToNull(request.getWorkcenterId()))
                .expectedInstallationDate(request.getExpectedInstallationDate())
                .fundingMethod(fundingMethod)
                .status("AWAITING_FUNDING")
                .fundingStatus("UNFUNDED")
                .productionStatus("NOT_STARTED")
                .installationStatus("NOT_READY")
                .discountCents(nonNegative(request.getDiscountCents()))
                .notes(trimToNull(request.getNotes()))
                .createdBy(systemActor(actor))
                .updatedBy(systemActor(actor))
                .build();
        order = orderRepository.save(order);
        replaceItems(order, request.getItems());
        order = recalculateOrderTotals(order);
        order = orderRepository.save(order);
        order = createOrderInvoice(order, systemActor(actor));
        saveHistory(order.getId(), "ORDER", null, order.getStatus(), "Tombstone order created", actor);
        saveHistory(order.getId(), "FUNDING", null, order.getFundingStatus(), "Funding not yet confirmed", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse updateOrder(String orderId, TombstoneDtos.UpdateOrderRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertOrderEditable(order);
        require(request, "Order update request is required");

        if (StringUtils.hasText(request.getCustomerPartnerId())) order.setCustomerPartnerId(request.getCustomerPartnerId().trim());
        if (request.getMembershipId() != null) order.setMembershipId(trimToNull(request.getMembershipId()));
        if (request.getDeceasedPartnerId() != null) order.setDeceasedPartnerId(trimToNull(request.getDeceasedPartnerId()));
        if (StringUtils.hasText(request.getDeceasedName())) order.setDeceasedName(request.getDeceasedName().trim());
        if (request.getFuneralServiceId() != null) order.setFuneralServiceId(trimToNull(request.getFuneralServiceId()));
        if (request.getCemeteryName() != null) order.setCemeteryName(trimToNull(request.getCemeteryName()));
        if (request.getCemeteryArea() != null) order.setCemeteryArea(trimToNull(request.getCemeteryArea()));
        if (request.getGraveNumber() != null) order.setGraveNumber(trimToNull(request.getGraveNumber()));
        if (request.getGraveLatitude() != null) order.setGraveLatitude(request.getGraveLatitude());
        if (request.getGraveLongitude() != null) order.setGraveLongitude(request.getGraveLongitude());
        if (request.getSalesArea() != null) order.setSalesArea(trimToNull(request.getSalesArea()));
        if (request.getWorkcenterId() != null) order.setWorkcenterId(trimToNull(request.getWorkcenterId()));
        if (request.getExpectedInstallationDate() != null) order.setExpectedInstallationDate(request.getExpectedInstallationDate());
        if (request.getFundingMethod() != null) {
            String method = normalizeRequired(request.getFundingMethod(), "fundingMethod");
            if (!FUNDING_METHODS.contains(method)) throw new IllegalArgumentException("Unsupported funding method: " + method);
            order.setFundingMethod(method);
        }
        if (request.getDiscountCents() != null) order.setDiscountCents(nonNegative(request.getDiscountCents()));
        if (request.getNotes() != null) order.setNotes(trimToNull(request.getNotes()));
        order.setUpdatedBy(systemActor(actor));
        order = orderRepository.save(order);

        if (request.getItems() != null) {
            if (request.getItems().isEmpty()) throw new IllegalArgumentException("At least one order item is required");
            replaceItems(order, request.getItems());
        }
        order = recalculateOrderTotals(order);
        order = recalculateFunding(order, actor);
        syncInvoice(order, systemActor(actor));
        return toResponse(orderRepository.save(order), true);
    }

    public List<TombstoneDtos.OrderResponse> getOrders(String status, String fundingStatus, String query) {
        String normalizedStatus = normalizeOptional(status);
        String normalizedFunding = normalizeOptional(fundingStatus);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> normalizedStatus == null || normalizedStatus.equals(o.getStatus()))
                .filter(o -> normalizedFunding == null || normalizedFunding.equals(o.getFundingStatus()))
                .filter(o -> q.isEmpty() || containsIgnoreCase(o.getOrderNo(), q)
                        || containsIgnoreCase(o.getDeceasedName(), q)
                        || containsIgnoreCase(o.getCemeteryName(), q)
                        || containsIgnoreCase(o.getGraveNumber(), q))
                .map(o -> toResponse(o, false))
                .toList();
    }

    public TombstoneDtos.OrderResponse getOrder(String orderId) {
        return toResponse(getOrderEntity(orderId), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse addFunding(String orderId, TombstoneDtos.FundingAllocationRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        require(request, "Funding allocation request is required");
        String fundingType = normalizeRequired(request.getFundingType(), "fundingType");
        if (!FUNDING_TYPES.contains(fundingType)) throw new IllegalArgumentException("Unsupported funding type: " + fundingType);
        if ("LAYBY".equals(fundingType)) {
            throw new IllegalArgumentException("Use the lay-by agreement endpoint to create lay-by funding");
        }
        long amount = positive(request.getAllocatedAmountCents(), "allocatedAmountCents");
        if (amount > safe(order.getBalanceCents())) {
            throw new IllegalArgumentException("Funding allocation exceeds the outstanding order balance");
        }

        TombstoneFundingAllocationEntity allocation;
        if ("CASH".equals(fundingType)) {
            allocation = addCashFunding(order, request, amount, actor);
        } else {
            allocation = addFuneralCoverFunding(order, request, amount, actor);
        }
        fundingRepository.save(allocation);
        if (!fundingType.equals(order.getFundingMethod())) {
            order.setFundingMethod("COMBINATION");
            order.setUpdatedBy(systemActor(actor));
            orderRepository.save(order);
        }
        order = recalculateFunding(order, actor);
        syncInvoice(order, systemActor(actor));
        return toResponse(orderRepository.save(order), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse createLayby(String orderId, TombstoneDtos.LaybyAgreementRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        require(request, "Lay-by agreement request is required");
        if (laybyRepository.findByTombstoneOrderId(orderId).isPresent()) {
            throw new IllegalStateException("A lay-by agreement already exists for this tombstone order");
        }
        long outstanding = safe(order.getBalanceCents());
        if (outstanding <= 0) throw new IllegalStateException("The tombstone order is already fully funded");
        long installmentAmount = positive(request.getInstallmentAmountCents(), "installmentAmountCents");
        long adminFee = nonNegative(request.getAdministrationFeeCents());
        long total = Math.addExact(outstanding, adminFee);
        long depositRequired = nonNegative(request.getDepositRequiredCents());
        if (depositRequired > total) {
            throw new IllegalArgumentException("depositRequiredCents cannot exceed the lay-by total");
        }
        String frequency = normalizeOptional(request.getPaymentFrequency());
        if (frequency == null) frequency = "MONTHLY";
        if (!Set.of("WEEKLY", "FORTNIGHTLY", "MONTHLY").contains(frequency)) {
            throw new IllegalArgumentException("paymentFrequency must be WEEKLY, FORTNIGHTLY or MONTHLY");
        }
        LocalDate startDate = request.getStartDate() == null ? LocalDate.now() : request.getStartDate();
        if (request.getExpectedSettlementDate() != null && request.getExpectedSettlementDate().isBefore(startDate)) {
            throw new IllegalArgumentException("expectedSettlementDate cannot be before startDate");
        }

        TombstoneLaybyAgreementEntity agreement = TombstoneLaybyAgreementEntity.builder()
                .agreementNo(formatNumber("TSL", numberAllocationService.allocateNumber("TOMBSTONE_LAYBY")))
                .tombstoneOrderId(orderId)
                .depositRequiredCents(depositRequired)
                .installmentAmountCents(installmentAmount)
                .paymentFrequency(frequency)
                .startDate(startDate)
                .expectedSettlementDate(request.getExpectedSettlementDate())
                .gracePeriodDays(request.getGracePeriodDays() == null ? 0 : Math.max(0, request.getGracePeriodDays()))
                .administrationFeeCents(adminFee)
                .totalAmountCents(total)
                .paidAmountCents(0L)
                .balanceCents(total)
                .status("ACTIVE")
                .termsAcceptedAt(LocalDateTime.now())
                .termsAcceptedBy(firstNonBlank(request.getTermsAcceptedBy(), systemActor(actor)))
                .createdBy(systemActor(actor))
                .updatedBy(systemActor(actor))
                .build();
        agreement = laybyRepository.save(agreement);
        generateInstallments(agreement);

        TombstoneFundingAllocationEntity allocation = TombstoneFundingAllocationEntity.builder()
                .tombstoneOrderId(orderId)
                .fundingType("LAYBY")
                .sourceType("LAYBY_AGREEMENT")
                .sourceId(agreement.getId())
                .sourceNo(agreement.getAgreementNo())
                .allocatedAmountCents(outstanding)
                .confirmedAmountCents(0L)
                .status("PENDING")
                .notes("Funding through lay-by agreement " + agreement.getAgreementNo())
                .createdBy(systemActor(actor))
                .updatedBy(systemActor(actor))
                .build();
        fundingRepository.save(allocation);
        order.setFundingMethod("LAYBY".equals(order.getFundingMethod()) ? "LAYBY" : "COMBINATION");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(orderId, "FUNDING", order.getFundingStatus(), order.getFundingStatus(), "Lay-by agreement created", actor);
        return toResponse(getOrderEntity(orderId), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse recordLaybyPayment(String agreementId, TombstoneDtos.LaybyPaymentRequest request, String actor) {
        TombstoneLaybyAgreementEntity agreement = laybyRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Lay-by agreement not found: " + agreementId));
        if (!Set.of("ACTIVE", "IN_ARREARS").contains(agreement.getStatus())) {
            throw new IllegalStateException("Payments cannot be captured against a " + agreement.getStatus() + " lay-by agreement");
        }
        require(request, "Lay-by payment request is required");
        requireText(request.getReceiptId(), "receiptId");
        long amount = positive(request.getAmountCents(), "amountCents");
        if (amount > safe(agreement.getBalanceCents())) amount = safe(agreement.getBalanceCents());

        ReceiptEntity receipt = getPostedReceipt(request.getReceiptId());
        assertReceiptAvailable(receipt, amount);
        if (receiptAllocationRepository.existsByReceiptIdAndAllocationTypeAndReferenceId(
                receipt.getId(), ReceiptAllocationType.TOMBSTONE_LAYBY, agreement.getId())) {
            throw new IllegalStateException("This receipt is already allocated to the lay-by agreement");
        }
        receiptAllocationRepository.save(ReceiptAllocationEntity.builder()
                .receiptId(receipt.getId())
                .allocationType(ReceiptAllocationType.TOMBSTONE_LAYBY)
                .referenceId(agreement.getId())
                .referenceNo(agreement.getAgreementNo())
                .amountCents(amount)
                .status(ReceiptStatus.POSTED)
                .createdBy(systemActor(actor))
                .build());

        applyPaymentToInstallments(agreement, receipt.getId(), amount);
        agreement.setPaidAmountCents(Math.addExact(safe(agreement.getPaidAmountCents()), amount));
        agreement.setBalanceCents(Math.max(0L, safe(agreement.getTotalAmountCents()) - agreement.getPaidAmountCents()));
        agreement.setStatus(agreement.getBalanceCents() == 0 ? "SETTLED" : "ACTIVE");
        agreement.setUpdatedBy(systemActor(actor));
        laybyRepository.save(agreement);

        TombstoneFundingAllocationEntity allocation = fundingRepository
                .findByTombstoneOrderIdAndSourceTypeAndSourceId(agreement.getTombstoneOrderId(), "LAYBY_AGREEMENT", agreement.getId())
                .orElseThrow(() -> new IllegalStateException("Lay-by funding allocation is missing"));
        long orderFunding = Math.min(safe(allocation.getAllocatedAmountCents()),
                Math.max(0L, safe(agreement.getPaidAmountCents()) - safe(agreement.getAdministrationFeeCents())));
        if (agreement.getBalanceCents() == 0) orderFunding = safe(allocation.getAllocatedAmountCents());
        allocation.setConfirmedAmountCents(orderFunding);
        allocation.setStatus(orderFunding >= safe(allocation.getAllocatedAmountCents()) ? "CONFIRMED" : "PARTIALLY_CONFIRMED");
        allocation.setConfirmedAt(orderFunding > 0 ? LocalDateTime.now() : null);
        allocation.setUpdatedBy(systemActor(actor));
        fundingRepository.save(allocation);

        TombstoneOrderEntity order = recalculateFunding(getOrderEntity(agreement.getTombstoneOrderId()), actor);
        syncInvoice(order, systemActor(actor));
        return toResponse(orderRepository.save(order), true);
    }

    public List<Map<String, Object>> getLaybyAgreements(String status) {
        String normalized = normalizeOptional(status);
        return laybyRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> normalized == null || normalized.equals(a.getStatus()))
                .map(this::laybyMap)
                .toList();
    }

    @Transactional
    public TombstoneDtos.OrderResponse addSiteAssessment(String orderId, TombstoneDtos.SiteAssessmentRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        require(request, "Site assessment request is required");
        int version = assessmentRepository.findFirstByTombstoneOrderIdOrderByVersionNoDesc(orderId)
                .map(a -> a.getVersionNo() + 1).orElse(1);
        String status = normalizeOptional(request.getStatus());
        if (status == null) status = request.getAssessedAt() == null ? "REQUESTED" : "COMPLETED";
        if (!Set.of("REQUESTED", "SCHEDULED", "COMPLETED", "FAILED", "REASSESSMENT_REQUIRED").contains(status)) {
            throw new IllegalArgumentException("Unsupported site assessment status: " + status);
        }
        TombstoneSiteAssessmentEntity assessment = TombstoneSiteAssessmentEntity.builder()
                .tombstoneOrderId(orderId).versionNo(version).status(status)
                .scheduledAt(request.getScheduledAt()).assessedAt(request.getAssessedAt())
                .assessorPartnerId(trimToNull(request.getAssessorPartnerId()))
                .cemeteryName(firstNonBlank(request.getCemeteryName(), order.getCemeteryName()))
                .graveNumber(firstNonBlank(request.getGraveNumber(), order.getGraveNumber()))
                .graveLatitude(request.getGraveLatitude() == null ? order.getGraveLatitude() : request.getGraveLatitude())
                .graveLongitude(request.getGraveLongitude() == null ? order.getGraveLongitude() : request.getGraveLongitude())
                .graveLengthMm(request.getGraveLengthMm()).graveWidthMm(request.getGraveWidthMm())
                .foundationCondition(trimToNull(request.getFoundationCondition()))
                .accessRestrictions(trimToNull(request.getAccessRestrictions()))
                .cemeteryRules(trimToNull(request.getCemeteryRules()))
                .permitRequired(Boolean.TRUE.equals(request.getPermitRequired()))
                .permitReference(trimToNull(request.getPermitReference()))
                .permitApproved(Boolean.TRUE.equals(request.getPermitApproved()))
                .travelDistanceKm(request.getTravelDistanceKm())
                .additionalWorkRequired(trimToNull(request.getAdditionalWorkRequired()))
                .additionalCostCents(nonNegative(request.getAdditionalCostCents()))
                .photoAttachmentIdsJson(toJson(request.getPhotoAttachmentIds()))
                .failureReason(trimToNull(request.getFailureReason()))
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
        assessment = assessmentRepository.save(assessment);

        order.setCemeteryName(firstNonBlank(assessment.getCemeteryName(), order.getCemeteryName()));
        order.setGraveNumber(firstNonBlank(assessment.getGraveNumber(), order.getGraveNumber()));
        order.setGraveLatitude(assessment.getGraveLatitude());
        order.setGraveLongitude(assessment.getGraveLongitude());
        String oldStatus = order.getStatus();
        if ("COMPLETED".equals(status)) {
            order.setStatus("DESIGN_PENDING");
            if (safe(assessment.getAdditionalCostCents()) > 0) {
                createAmendmentInternal(orderId,
                        "Additional site work from assessment version " + version,
                        assessment.getAdditionalCostCents(), null, actor);
            }
        } else {
            order.setStatus("SITE_ASSESSMENT_PENDING");
        }
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(orderId, "ORDER", oldStatus, order.getStatus(), "Site assessment " + status.toLowerCase(Locale.ROOT), actor);
        return toResponse(order, true);
    }

    public List<Map<String, Object>> getAssessments(String status) {
        String normalized = normalizeOptional(status);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .flatMap(o -> assessmentRepository.findByTombstoneOrderIdOrderByVersionNoDesc(o.getId()).stream())
                .filter(a -> normalized == null || normalized.equals(a.getStatus()))
                .map(this::assessmentMap).toList();
    }

    @Transactional
    public TombstoneDtos.OrderResponse createAmendment(String orderId, TombstoneDtos.AmendmentRequest request, String actor) {
        require(request, "Order amendment request is required");
        requireText(request.getReason(), "reason");
        createAmendmentInternal(orderId, request.getReason(), request.getAmountDeltaCents(), request.getSupportingAttachmentId(), actor);
        return toResponse(getOrderEntity(orderId), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse decideAmendment(String amendmentId, TombstoneDtos.AmendmentDecisionRequest request, String actor) {
        TombstoneOrderAmendmentEntity amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new IllegalArgumentException("Tombstone order amendment not found: " + amendmentId));
        if (!"PENDING_CUSTOMER_APPROVAL".equals(amendment.getStatus())) {
            throw new IllegalStateException("The amendment has already been decided");
        }
        String decision = normalizeRequired(request == null ? null : request.getDecision(), "decision");
        String actionBy = firstNonBlank(request == null ? null : request.getActionBy(), actor);
        TombstoneOrderEntity order = getOrderEntity(amendment.getTombstoneOrderId());
        amendment.setResponseNotes(request == null ? null : trimToNull(request.getResponseNotes()));
        if ("APPROVED".equals(decision)) {
            amendment.setStatus("APPROVED");
            amendment.setApprovedAt(LocalDateTime.now());
            amendment.setApprovedBy(systemActor(actionBy));
            amendmentRepository.saveAndFlush(amendment);

            order = recalculateOrderTotals(order);
            order.setUpdatedBy(systemActor(actionBy));
            orderRepository.save(order);
            order = recalculateFunding(order, actionBy);
            syncInvoice(order, systemActor(actionBy));
            saveHistory(order.getId(), "ORDER", order.getStatus(), order.getStatus(),
                    "Order amendment " + amendment.getAmendmentNo() + " approved", actionBy);
        } else if ("REJECTED".equals(decision)) {
            amendment.setStatus("REJECTED");
            amendment.setRejectedAt(LocalDateTime.now());
            amendment.setRejectedBy(systemActor(actionBy));
            amendmentRepository.save(amendment);
        } else {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }
        return toResponse(getOrderEntity(order.getId()), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse addDesign(String orderId, TombstoneDtos.DesignRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        require(request, "Design request is required");
        int version = designRepository.findFirstByTombstoneOrderIdOrderByVersionNoDesc(orderId)
                .map(d -> d.getVersionNo() + 1).orElse(1);
        String status = normalizeOptional(request.getStatus());
        if (status == null) status = "DRAFT";
        if (!Set.of("DRAFT", "SENT_FOR_APPROVAL", "CHANGES_REQUESTED").contains(status)) {
            throw new IllegalArgumentException("New design status must be DRAFT, SENT_FOR_APPROVAL or CHANGES_REQUESTED");
        }
        TombstoneDesignEntity design = TombstoneDesignEntity.builder()
                .tombstoneOrderId(orderId).versionNo(version).status(status)
                .inscriptionText(trimToNull(request.getInscriptionText()))
                .fontName(trimToNull(request.getFontName()))
                .layoutNotes(trimToNull(request.getLayoutNotes()))
                .symbolsJson(toJson(request.getSymbols()))
                .material(trimToNull(request.getMaterial()))
                .colour(trimToNull(request.getColour()))
                .dimensions(trimToNull(request.getDimensions()))
                .designAttachmentId(trimToNull(request.getDesignAttachmentId()))
                .sentForApprovalAt("SENT_FOR_APPROVAL".equals(status) ? LocalDateTime.now() : null)
                .changeRequest(trimToNull(request.getChangeRequest()))
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
        designRepository.save(design);
        String old = order.getStatus();
        order.setStatus("DESIGN_PENDING");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(orderId, "ORDER", old, order.getStatus(), "Design version " + version + " created", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse approveDesign(String designId, TombstoneDtos.DesignApprovalRequest request, String actor) {
        TombstoneDesignEntity design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("Tombstone design not found: " + designId));
        TombstoneOrderEntity order = getOrderEntity(design.getTombstoneOrderId());
        requireCompletedAssessment(order.getId());
        if (!StringUtils.hasText(design.getDesignAttachmentId()) && !StringUtils.hasText(design.getInscriptionText())) {
            throw new IllegalStateException("A design attachment or inscription is required before approval");
        }
        for (TombstoneDesignEntity previous : designRepository.findByTombstoneOrderIdOrderByVersionNoDesc(order.getId())) {
            if (!previous.getId().equals(designId) && "APPROVED".equals(previous.getStatus())) {
                previous.setStatus("SUPERSEDED");
                previous.setUpdatedBy(systemActor(actor));
                designRepository.save(previous);
            }
        }
        design.setStatus("APPROVED");
        design.setCustomerApprovalMethod(firstNonBlank(request == null ? null : request.getApprovalMethod(), "SIGNATURE"));
        design.setCustomerApprovalReference(trimToNull(request == null ? null : request.getApprovalReference()));
        design.setApprovedAt(LocalDateTime.now());
        design.setApprovedBy(systemActor(firstNonBlank(request == null ? null : request.getApprovedBy(), actor)));
        design.setUpdatedBy(systemActor(actor));
        designRepository.save(design);
        String old = order.getStatus();
        order.setStatus("DESIGN_APPROVED");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "ORDER", old, order.getStatus(), "Customer approved design version " + design.getVersionNo(), actor);
        return toResponse(order, true);
    }

    public List<Map<String, Object>> getDesigns(String status) {
        String normalized = normalizeOptional(status);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .flatMap(o -> designRepository.findByTombstoneOrderIdOrderByVersionNoDesc(o.getId()).stream())
                .filter(d -> normalized == null || normalized.equals(d.getStatus()))
                .map(this::designMap).toList();
    }

    @Transactional
    public TombstoneDtos.OrderResponse createProductionJob(String orderId, TombstoneDtos.ProductionJobRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        requireFullyFunded(order);
        requireCompletedAssessment(orderId);
        if (amendmentRepository.countByTombstoneOrderIdAndStatus(orderId, "PENDING_CUSTOMER_APPROVAL") > 0) {
            throw new IllegalStateException("Pending order amendments must be approved or rejected before production starts");
        }
        TombstoneDesignEntity design;
        if (request != null && StringUtils.hasText(request.getDesignId())) {
            design = designRepository.findById(request.getDesignId())
                    .orElseThrow(() -> new IllegalArgumentException("Design not found: " + request.getDesignId()));
            if (!orderId.equals(design.getTombstoneOrderId()) || !"APPROVED".equals(design.getStatus())) {
                throw new IllegalStateException("Production requires an approved design for this tombstone order");
            }
        } else {
            design = designRepository.findFirstByTombstoneOrderIdAndStatusOrderByVersionNoDesc(orderId, "APPROVED")
                    .orElseThrow(() -> new IllegalStateException("An approved design is required before production"));
        }
        boolean hasActiveProductionJob = productionRepository.findByTombstoneOrderIdOrderByCreatedAtDesc(orderId).stream()
                .anyMatch(existing -> !"CANCELLED".equals(existing.getStatus()));
        if (hasActiveProductionJob) {
            throw new IllegalStateException("An active production job already exists for this tombstone order");
        }
        boolean internal = request == null || request.getInternalProduction() == null || request.getInternalProduction();
        if (!internal && (request == null || !StringUtils.hasText(request.getSupplierPartnerId()))) {
            throw new IllegalArgumentException("supplierPartnerId is required for external production");
        }
        TombstoneProductionJobEntity job = TombstoneProductionJobEntity.builder()
                .jobNo(formatNumber("TSP", numberAllocationService.allocateNumber("TOMBSTONE_PRODUCTION")))
                .tombstoneOrderId(orderId).designId(design.getId()).internalProduction(internal)
                .supplierPartnerId(trimToNull(request == null ? null : request.getSupplierPartnerId()))
                .purchaseOrderId(trimToNull(request == null ? null : request.getPurchaseOrderId()))
                .status("MATERIAL_ORDERED")
                .plannedStartDate(request == null ? null : request.getPlannedStartDate())
                .plannedCompletionDate(request == null ? null : request.getPlannedCompletionDate())
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
        productionRepository.save(job);
        String oldOrder = order.getStatus();
        String oldProduction = order.getProductionStatus();
        order.setStatus("IN_PRODUCTION");
        order.setProductionStatus(job.getStatus());
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(orderId, "ORDER", oldOrder, order.getStatus(), "Production job " + job.getJobNo() + " created", actor);
        saveHistory(orderId, "PRODUCTION", oldProduction, job.getStatus(), "Production started", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse updateProductionStatus(String jobId, TombstoneDtos.StatusUpdateRequest request, String actor) {
        TombstoneProductionJobEntity job = productionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Production job not found: " + jobId));
        String status = normalizeRequired(request == null ? null : request.getStatus(), "status");
        if (!PRODUCTION_STATUSES.contains(status)) throw new IllegalArgumentException("Unsupported production status: " + status);
        String old = job.getStatus();
        job.setStatus(status);
        if ("MATERIAL_RECEIVED".equals(status) && job.getActualStartAt() == null) job.setActualStartAt(LocalDateTime.now());
        if ("QUALITY_CHECK".equals(status) || "READY_FOR_INSTALLATION".equals(status)) {
            if (!StringUtils.hasText(request.getQualityCheckedBy())) {
                throw new IllegalArgumentException("qualityCheckedBy is required for quality completion");
            }
            job.setQualityCheckedAt(LocalDateTime.now());
            job.setQualityCheckedBy(request.getQualityCheckedBy());
            job.setQualityNotes(trimToNull(request.getQualityNotes()));
        }
        if ("READY_FOR_INSTALLATION".equals(status)) job.setActualCompletionAt(LocalDateTime.now());
        job.setUpdatedBy(systemActor(actor));
        productionRepository.save(job);

        TombstoneOrderEntity order = getOrderEntity(job.getTombstoneOrderId());
        String oldOrder = order.getStatus();
        String oldProduction = order.getProductionStatus();
        order.setProductionStatus(status);
        if ("READY_FOR_INSTALLATION".equals(status)) order.setStatus("READY_FOR_INSTALLATION");
        if ("CANCELLED".equals(status)) order.setStatus("DESIGN_APPROVED");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "PRODUCTION", oldProduction, status, request.getReason(), actor);
        if (!Objects.equals(oldOrder, order.getStatus())) saveHistory(order.getId(), "ORDER", oldOrder, order.getStatus(), request.getReason(), actor);
        return toResponse(order, true);
    }

    public List<Map<String, Object>> getProductionJobs(String status) {
        String normalized = normalizeOptional(status);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .flatMap(o -> productionRepository.findByTombstoneOrderIdOrderByCreatedAtDesc(o.getId()).stream())
                .filter(j -> normalized == null || normalized.equals(j.getStatus()))
                .map(this::productionMap).toList();
    }

    @Transactional
    public PaymentRequestResponse createSupplierPaymentRequest(String jobId, TombstoneDtos.SupplierPaymentRequest request, String actor) {
        TombstoneProductionJobEntity job = productionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Production job not found: " + jobId));
        if (Boolean.TRUE.equals(job.getInternalProduction())) {
            throw new IllegalStateException("Supplier payments only apply to externally produced tombstones");
        }
        if (!Set.of("QUALITY_CHECK", "READY_FOR_INSTALLATION").contains(job.getStatus())) {
            throw new IllegalStateException("Supplier payment can only be requested after the production quality milestone");
        }
        require(request, "Supplier payment request is required");
        String milestone = firstNonBlank(normalizeOptional(request.getMilestone()), "PRODUCTION_QUALITY_MILESTONE");
        String externalReference = job.getJobNo() + ":" + milestone;
        String idempotencyKey = "TOMBSTONE_SUPPLIER:" + job.getId() + ":" + milestone;
        Optional<za.co.mawa.bes.entity.v2.PaymentRequestEntity> existingPayment = paymentRequestRepository
                .findFirstBySourceTypeAndSourceIdAndRequestTypeAndExternalReferenceOrderByCreatedAtAsc(
                        PaymentRequestSourceType.TOMBSTONE_ORDER,
                        job.getTombstoneOrderId(),
                        PaymentRequestType.SUPPLIER_INVOICE,
                        externalReference
                );
        if (existingPayment.isPresent()) {
            PaymentRequestResponse existingResponse = paymentRequestService.getById(existingPayment.get().getId());
            if (existingResponse.getStatus() == PaymentRequestStatus.DRAFT) {
                return paymentRequestService.submit(existingResponse.getId(), systemActor(actor));
            }
            return existingResponse;
        }
        long amount = positive(request.getAmountCents(), "amountCents");
        PaymentMethod method = PaymentMethod.valueOf(normalizeRequired(request.getPaymentMethod(), "paymentMethod"));
        PaymentRequestCreateRequest payment = new PaymentRequestCreateRequest();
        payment.setRequestType(PaymentRequestType.SUPPLIER_INVOICE);
        payment.setSourceType(PaymentRequestSourceType.TOMBSTONE_ORDER);
        payment.setSourceId(job.getTombstoneOrderId());
        payment.setPayeePartnerId(job.getSupplierPartnerId());
        payment.setPayeeName(firstNonBlank(request.getPayeeName(), "Tombstone supplier"));
        payment.setAmount(BigDecimal.valueOf(amount, 2));
        payment.setCurrency("ZAR");
        payment.setPaymentMethod(method);
        payment.setBankName(request.getBankName());
        payment.setAccountHolder(request.getAccountHolder());
        payment.setAccountNumber(request.getAccountNumber());
        payment.setBranchCode(request.getBranchCode());
        payment.setAccountType(request.getAccountType());
        payment.setExternalReference(externalReference);
        payment.setPaymentReason(milestone);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setNotes(request.getNotes());
        payment.setRequestedPaymentDate(LocalDate.now());
        PaymentRequestResponse response = paymentRequestService.create(payment, systemActor(actor));
        return paymentRequestService.submit(response.getId(), systemActor(actor));
    }

    @Transactional
    public TombstoneDtos.OrderResponse createInstallation(String orderId, TombstoneDtos.InstallationRequest request, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        assertNotClosed(order);
        requireFullyFunded(order);
        TombstoneProductionJobEntity job = resolveReadyProductionJob(orderId, request == null ? null : request.getProductionJobId());
        TombstoneSiteAssessmentEntity assessment = requireCompletedAssessment(orderId);
        if (Boolean.TRUE.equals(assessment.getPermitRequired()) && !Boolean.TRUE.equals(assessment.getPermitApproved())) {
            throw new IllegalStateException("The cemetery installation permit must be approved before scheduling");
        }
        require(request, "Installation request is required");
        boolean hasActivePrimaryInstallation = installationRepository.findByTombstoneOrderIdOrderByCreatedAtDesc(orderId).stream()
                .anyMatch(existing -> existing.getReworkOfInstallationId() == null
                        && !Set.of("CANCELLED", "COMPLETED").contains(existing.getStatus()));
        if (hasActivePrimaryInstallation) {
            throw new IllegalStateException("An active installation plan already exists for this tombstone order");
        }
        if (request.getScheduledStartAt() != null && request.getScheduledEndAt() != null
                && request.getScheduledEndAt().isBefore(request.getScheduledStartAt())) {
            throw new IllegalArgumentException("scheduledEndAt cannot be before scheduledStartAt");
        }
        String status = request.getScheduledStartAt() == null ? "READY_TO_SCHEDULE" : "SCHEDULED";
        TombstoneInstallationEntity installation = TombstoneInstallationEntity.builder()
                .installationNo(formatNumber("TSI", numberAllocationService.allocateNumber("TOMBSTONE_INSTALLATION")))
                .tombstoneOrderId(orderId).productionJobId(job.getId()).status(status)
                .scheduledStartAt(request.getScheduledStartAt()).scheduledEndAt(request.getScheduledEndAt())
                .cemeteryName(firstNonBlank(request.getCemeteryName(), order.getCemeteryName()))
                .graveNumber(firstNonBlank(request.getGraveNumber(), order.getGraveNumber()))
                .assignedVehicleId(trimToNull(request.getAssignedVehicleId()))
                .contactPerson(trimToNull(request.getContactPerson()))
                .contactNumber(trimToNull(request.getContactNumber()))
                .permitReference(firstNonBlank(request.getPermitReference(), assessment.getPermitReference()))
                .instructions(trimToNull(request.getInstructions()))
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
        installation = installationRepository.save(installation);
        replaceInstallationTeam(installation.getId(), request.getTeam());
        replaceInstallationMaterials(installation.getId(), request.getMaterials());
        createDefaultChecklist(installation.getId());

        String oldOrder = order.getStatus();
        String oldInstallation = order.getInstallationStatus();
        order.setStatus("SCHEDULED".equals(status) ? "INSTALLATION_SCHEDULED" : "READY_FOR_INSTALLATION");
        order.setInstallationStatus(status);
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(orderId, "INSTALLATION", oldInstallation, status, "Installation " + installation.getInstallationNo() + " created", actor);
        saveHistory(orderId, "ORDER", oldOrder, order.getStatus(), "Installation planning created", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse updateInstallationStatus(String installationId, TombstoneDtos.StatusUpdateRequest request, String actor) {
        TombstoneInstallationEntity installation = getInstallationEntity(installationId);
        String status = normalizeRequired(request == null ? null : request.getStatus(), "status");
        if (!INSTALLATION_OPERATIONAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Use the completion and acceptance endpoints for INSTALLED or COMPLETED statuses");
        }
        String old = installation.getStatus();
        if ("SCHEDULED".equals(status)) {
            LocalDateTime start = request.getScheduledStartAt() == null
                    ? installation.getScheduledStartAt() : request.getScheduledStartAt();
            LocalDateTime end = request.getScheduledEndAt() == null
                    ? installation.getScheduledEndAt() : request.getScheduledEndAt();
            if (start == null) {
                throw new IllegalArgumentException("scheduledStartAt is required when scheduling an installation");
            }
            if (end != null && end.isBefore(start)) {
                throw new IllegalArgumentException("scheduledEndAt cannot be before scheduledStartAt");
            }
            installation.setScheduledStartAt(start);
            installation.setScheduledEndAt(end);
        }
        installation.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        if ("TEAM_DISPATCHED".equals(status)) installation.setDispatchedAt(now);
        if ("ON_SITE".equals(status)) installation.setArrivedAt(now);
        if ("REWORK_REQUIRED".equals(status)) installation.setReworkReason(trimToNull(request.getReason()));
        installation.setUpdatedBy(systemActor(actor));
        installationRepository.save(installation);

        TombstoneOrderEntity order = getOrderEntity(installation.getTombstoneOrderId());
        String oldOrder = order.getStatus();
        String oldInstallation = order.getInstallationStatus();
        order.setInstallationStatus(status);
        order.setStatus(switch (status) {
            case "REWORK_REQUIRED" -> "REWORK_REQUIRED";
            case "SCHEDULED" -> "INSTALLATION_SCHEDULED";
            default -> order.getStatus();
        });
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "INSTALLATION", oldInstallation, status, request.getReason(), actor);
        if (!Objects.equals(oldOrder, order.getStatus())) saveHistory(order.getId(), "ORDER", oldOrder, order.getStatus(), request.getReason(), actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse updateChecklist(String installationId, String checklistId,
                                                       TombstoneDtos.ChecklistUpdateRequest request, String actor) {
        TombstoneInstallationEntity installation = getInstallationEntity(installationId);
        TombstoneInstallationChecklistEntity item = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new IllegalArgumentException("Installation checklist item not found: " + checklistId));
        if (!installationId.equals(item.getInstallationId())) throw new IllegalArgumentException("Checklist item does not belong to the installation");
        boolean completed = request != null && Boolean.TRUE.equals(request.getCompleted());
        item.setCompleted(completed);
        item.setCompletedAt(completed ? LocalDateTime.now() : null);
        item.setCompletedBy(completed ? systemActor(actor) : null);
        item.setNotes(trimToNull(request == null ? null : request.getNotes()));
        item.setEvidenceAttachmentId(trimToNull(request == null ? null : request.getEvidenceAttachmentId()));
        checklistRepository.save(item);
        return toResponse(getOrderEntity(installation.getTombstoneOrderId()), true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse completeInstallation(String installationId,
                                                            TombstoneDtos.InstallationCompletionRequest request,
                                                            String actor) {
        TombstoneInstallationEntity installation = getInstallationEntity(installationId);
        if (!"ON_SITE".equals(installation.getStatus())) {
            throw new IllegalStateException("The installation must be ON_SITE before it can be completed");
        }
        if (checklistRepository.countByInstallationIdAndRequiredTrueAndCompletedFalse(installationId) > 0) {
            throw new IllegalStateException("All required installation checklist items must be completed");
        }
        require(request, "Installation completion request is required");
        if (request.getBeforePhotoAttachmentIds() == null || request.getBeforePhotoAttachmentIds().isEmpty()) {
            throw new IllegalArgumentException("At least one before-installation photograph is required");
        }
        if (request.getAfterPhotoAttachmentIds() == null || request.getAfterPhotoAttachmentIds().isEmpty()) {
            throw new IllegalArgumentException("At least one after-installation photograph is required");
        }
        requireText(request.getCustomerRepresentativeName(), "customerRepresentativeName");
        requireText(request.getCustomerSignatureAttachmentId(), "customerSignatureAttachmentId");
        requireText(request.getInstallerSignatureAttachmentId(), "installerSignatureAttachmentId");
        installation.setBeforePhotoAttachmentIdsJson(toJson(request.getBeforePhotoAttachmentIds()));
        installation.setAfterPhotoAttachmentIdsJson(toJson(request.getAfterPhotoAttachmentIds()));
        installation.setCustomerRepresentativeName(request.getCustomerRepresentativeName().trim());
        installation.setCustomerSignatureAttachmentId(trimToNull(request.getCustomerSignatureAttachmentId()));
        installation.setInstallerSignatureAttachmentId(request.getInstallerSignatureAttachmentId().trim());
        installation.setCompletionNotes(trimToNull(request.getCompletionNotes()));
        installation.setInstalledAt(LocalDateTime.now());
        installation.setStatus("INSTALLED");
        installation.setUpdatedBy(systemActor(actor));
        installationRepository.save(installation);

        TombstoneOrderEntity order = getOrderEntity(installation.getTombstoneOrderId());
        String oldOrder = order.getStatus();
        String oldInstallation = order.getInstallationStatus();
        order.setStatus("INSTALLED");
        order.setInstallationStatus("INSTALLED");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "INSTALLATION", oldInstallation, "INSTALLED", "Installation checklist completed", actor);
        saveHistory(order.getId(), "ORDER", oldOrder, "INSTALLED", "Tombstone installed", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse acceptInstallation(String installationId, TombstoneDtos.AcceptanceRequest request, String actor) {
        TombstoneInstallationEntity installation = getInstallationEntity(installationId);
        if (!Set.of("INSTALLED", "CUSTOMER_ACCEPTED").contains(installation.getStatus())) {
            throw new IllegalStateException("Only an installed tombstone can be accepted");
        }
        installation.setCustomerRepresentativeName(firstNonBlank(request == null ? null : request.getAcceptedBy(), installation.getCustomerRepresentativeName()));
        installation.setAcceptedAt(LocalDateTime.now());
        installation.setCompletedAt(LocalDateTime.now());
        installation.setCompletionNotes(joinNotes(installation.getCompletionNotes(), request == null ? null : request.getNotes()));
        installation.setStatus("COMPLETED");
        installation.setUpdatedBy(systemActor(actor));
        installationRepository.save(installation);
        TombstoneOrderEntity order = getOrderEntity(installation.getTombstoneOrderId());
        String oldOrder = order.getStatus();
        String oldInstallation = order.getInstallationStatus();
        order.setStatus("COMPLETED");
        order.setInstallationStatus("COMPLETED");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "INSTALLATION", oldInstallation, "COMPLETED", "Customer/supervisor accepted installation", actor);
        saveHistory(order.getId(), "ORDER", oldOrder, "COMPLETED", "Tombstone order completed", actor);
        return toResponse(order, true);
    }

    @Transactional
    public TombstoneDtos.OrderResponse createRework(String installationId, TombstoneDtos.ReworkRequest request, String actor) {
        TombstoneInstallationEntity original = getInstallationEntity(installationId);
        if (!Set.of("INSTALLED", "CUSTOMER_ACCEPTED", "COMPLETED", "REWORK_REQUIRED").contains(original.getStatus())) {
            throw new IllegalStateException("Rework can only be created after an installation has been completed or inspected");
        }
        require(request, "Rework request is required");
        requireText(request.getReason(), "reason");
        if (request.getScheduledStartAt() != null && request.getScheduledEndAt() != null
                && request.getScheduledEndAt().isBefore(request.getScheduledStartAt())) {
            throw new IllegalArgumentException("scheduledEndAt cannot be before scheduledStartAt");
        }
        boolean activeReworkExists = installationRepository
                .findByTombstoneOrderIdOrderByCreatedAtDesc(original.getTombstoneOrderId()).stream()
                .anyMatch(candidate -> original.getId().equals(candidate.getReworkOfInstallationId())
                        && !Set.of("CANCELLED", "COMPLETED").contains(candidate.getStatus()));
        if (activeReworkExists) {
            throw new IllegalStateException("An active rework installation already exists");
        }
        original.setStatus("REWORK_REQUIRED");
        original.setReworkReason(request.getReason().trim());
        original.setUpdatedBy(systemActor(actor));
        installationRepository.save(original);

        TombstoneInstallationEntity rework = TombstoneInstallationEntity.builder()
                .installationNo(formatNumber("TSI", numberAllocationService.allocateNumber("TOMBSTONE_INSTALLATION")))
                .tombstoneOrderId(original.getTombstoneOrderId())
                .productionJobId(original.getProductionJobId())
                .reworkOfInstallationId(original.getId())
                .status(request.getScheduledStartAt() == null ? "READY_TO_SCHEDULE" : "SCHEDULED")
                .scheduledStartAt(request.getScheduledStartAt()).scheduledEndAt(request.getScheduledEndAt())
                .cemeteryName(original.getCemeteryName()).graveNumber(original.getGraveNumber())
                .assignedVehicleId(original.getAssignedVehicleId())
                .contactPerson(original.getContactPerson()).contactNumber(original.getContactNumber())
                .permitReference(original.getPermitReference())
                .instructions("Rework: " + request.getReason().trim())
                .reworkReason(request.getReason().trim())
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
        rework = installationRepository.save(rework);
        copyInstallationResources(original.getId(), rework.getId());
        createDefaultChecklist(rework.getId());

        TombstoneOrderEntity order = getOrderEntity(original.getTombstoneOrderId());
        String oldOrder = order.getStatus();
        String oldInstallation = order.getInstallationStatus();
        order.setStatus("REWORK_REQUIRED");
        order.setInstallationStatus("REWORK_REQUIRED");
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        saveHistory(order.getId(), "INSTALLATION", oldInstallation, "REWORK_REQUIRED", request.getReason(), actor);
        saveHistory(order.getId(), "ORDER", oldOrder, "REWORK_REQUIRED", request.getReason(), actor);
        return toResponse(order, true);
    }

    public List<Map<String, Object>> getInstallations(String status) {
        String normalized = normalizeOptional(status);
        return installationRepository.findAllByOrderByScheduledStartAtAsc().stream()
                .filter(i -> normalized == null || normalized.equals(i.getStatus()))
                .map(this::installationMap).toList();
    }

    @Transactional
    public Map<String, Object> cancelOrder(String orderId, String reason, String actor) {
        TombstoneOrderEntity order = getOrderEntity(orderId);
        if ("COMPLETED".equals(order.getStatus())) throw new IllegalStateException("A completed tombstone order cannot be cancelled");
        if ("CANCELLED".equals(order.getStatus())) return cancellationSummary(order);
        requireText(reason, "cancellation reason");
        String old = order.getStatus();
        order.setStatus("CANCELLED");
        order.setCancellationReason(reason.trim());
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        for (TombstoneFundingAllocationEntity allocation : fundingRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(orderId)) {
            if ("MEMBERSHIP_CLAIM".equals(allocation.getSourceType()) && StringUtils.hasText(allocation.getSourceId())) {
                claimRepository.findById(allocation.getSourceId()).ifPresent(claim -> {
                    if (orderId.equals(claim.getTombstoneOrderId())) {
                        claim.setTombstoneOrderId(null);
                        claim.setSettlementMethod(null);
                        claim.setSettlementReference(null);
                        claim.setSettledAt(null);
                        claim.setStatus(MembershipClaimStatus.APPROVED);
                        claim.setUpdatedBy(systemActor(actor));
                        claimRepository.save(claim);
                    }
                });
                allocation.setStatus("RELEASED");
                allocation.setConfirmedAmountCents(0L);
                allocation.setConfirmedAt(null);
            } else if (safe(allocation.getConfirmedAmountCents()) > 0) {
                allocation.setStatus("REFUND_REQUIRED");
            } else if (!"CANCELLED".equals(allocation.getStatus())) {
                allocation.setStatus("CANCELLED");
            }
            allocation.setUpdatedBy(systemActor(actor));
            fundingRepository.save(allocation);
        }
        laybyRepository.findByTombstoneOrderId(orderId).ifPresent(agreement -> {
            agreement.setStatus("CANCELLED");
            agreement.setCancellationReason(reason.trim());
            agreement.setUpdatedBy(systemActor(actor));
            laybyRepository.save(agreement);
        });
        saveHistory(orderId, "ORDER", old, "CANCELLED", reason, actor);
        return cancellationSummary(order);
    }

    public TombstoneDtos.DashboardResponse getDashboard() {
        List<TombstoneOrderEntity> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        long outstandingLayby = laybyRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> Set.of("ACTIVE", "IN_ARREARS").contains(a.getStatus()))
                .mapToLong(a -> safe(a.getBalanceCents())).sum();
        return TombstoneDtos.DashboardResponse.builder()
                .totalOrders(orders.size())
                .awaitingFunding(count(orders, o -> "AWAITING_FUNDING".equals(o.getStatus())))
                .partiallyFunded(count(orders, o -> "PARTIALLY_FUNDED".equals(o.getFundingStatus())))
                .fullyFunded(count(orders, o -> "FULLY_FUNDED".equals(o.getFundingStatus())))
                .assessmentPending(count(orders, o -> "SITE_ASSESSMENT_PENDING".equals(o.getStatus())))
                .designPending(count(orders, o -> "DESIGN_PENDING".equals(o.getStatus())))
                .inProduction(count(orders, o -> "IN_PRODUCTION".equals(o.getStatus())))
                .readyForInstallation(count(orders, o -> "READY_FOR_INSTALLATION".equals(o.getStatus())))
                .scheduledInstallations(count(orders, o -> "INSTALLATION_SCHEDULED".equals(o.getStatus())))
                .reworkRequired(count(orders, o -> "REWORK_REQUIRED".equals(o.getStatus())))
                .completed(count(orders, o -> "COMPLETED".equals(o.getStatus())))
                .outstandingLaybyCents(outstandingLayby)
                .build();
    }

    private TombstoneFundingAllocationEntity addCashFunding(TombstoneOrderEntity order,
                                                             TombstoneDtos.FundingAllocationRequest request,
                                                             long amount, String actor) {
        requireText(request.getSourceId(), "sourceId (receiptId)");
        ReceiptEntity receipt = getPostedReceipt(request.getSourceId());
        assertReceiptAvailable(receipt, amount);
        if (fundingRepository.findByTombstoneOrderIdAndSourceTypeAndSourceId(order.getId(), "RECEIPT", receipt.getId()).isPresent()) {
            throw new IllegalStateException("The receipt is already allocated to this tombstone order");
        }
        receiptAllocationRepository.save(ReceiptAllocationEntity.builder()
                .receiptId(receipt.getId()).allocationType(ReceiptAllocationType.TOMBSTONE_ORDER)
                .referenceId(order.getId()).referenceNo(order.getOrderNo())
                .amountCents(amount).status(ReceiptStatus.POSTED).createdBy(systemActor(actor)).build());
        return TombstoneFundingAllocationEntity.builder()
                .tombstoneOrderId(order.getId()).fundingType("CASH").sourceType("RECEIPT")
                .sourceId(receipt.getId()).sourceNo(receipt.getReceiptNo())
                .allocatedAmountCents(amount).confirmedAmountCents(amount).status("CONFIRMED")
                .confirmedAt(LocalDateTime.now()).notes(trimToNull(request.getNotes()))
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
    }

    private TombstoneFundingAllocationEntity addFuneralCoverFunding(TombstoneOrderEntity order,
                                                                     TombstoneDtos.FundingAllocationRequest request,
                                                                     long amount, String actor) {
        requireText(request.getSourceId(), "sourceId (membershipClaimId)");
        MembershipClaimEntity claim = claimRepository.findById(request.getSourceId())
                .orElseThrow(() -> new IllegalArgumentException("Membership claim not found: " + request.getSourceId()));
        if (!Set.of(MembershipClaimType.TOMBSTONE, MembershipClaimType.COMBINATION).contains(claim.getClaimType())) {
            throw new IllegalArgumentException("Only TOMBSTONE or COMBINATION claims can fund a tombstone order");
        }
        if (!Set.of(MembershipClaimStatus.APPROVED, MembershipClaimStatus.PAYMENT_PENDING).contains(claim.getStatus())) {
            throw new IllegalStateException("The tombstone claim must be approved before it can fund an order");
        }
        if (StringUtils.hasText(claim.getTombstoneOrderId()) && !order.getId().equals(claim.getTombstoneOrderId())) {
            throw new IllegalStateException("The claim has already been settled against another tombstone order");
        }
        long approved = claim.getApprovedAmountCents() != null && claim.getApprovedAmountCents() > 0
                ? claim.getApprovedAmountCents() : safe(claim.getClaimAmountCents());
        long alreadyUsed = fundingRepository.findBySourceTypeAndSourceId("MEMBERSHIP_CLAIM", claim.getId()).stream()
                .mapToLong(a -> safe(a.getConfirmedAmountCents())).sum();
        if (amount > Math.max(0L, approved - alreadyUsed)) {
            throw new IllegalArgumentException("Funding allocation exceeds the available approved tombstone benefit");
        }
        claim.setTombstoneOrderId(order.getId());
        claim.setSettlementMethod("INTERNAL_TOMBSTONE_ORDER");
        claim.setSettlementReference(order.getId());
        claim.setSettledAt(LocalDateTime.now());
        claim.setStatus(MembershipClaimStatus.PAID);
        claim.setUpdatedBy(systemActor(actor));
        claimRepository.save(claim);
        return TombstoneFundingAllocationEntity.builder()
                .tombstoneOrderId(order.getId()).fundingType("FUNERAL_COVER").sourceType("MEMBERSHIP_CLAIM")
                .sourceId(claim.getId()).sourceNo(claim.getClaimNo())
                .allocatedAmountCents(amount).confirmedAmountCents(amount).status("CONFIRMED")
                .confirmedAt(LocalDateTime.now()).notes(trimToNull(request.getNotes()))
                .createdBy(systemActor(actor)).updatedBy(systemActor(actor)).build();
    }

    private TombstoneOrderEntity recalculateOrderTotals(TombstoneOrderEntity order) {
        List<TombstoneOrderItemEntity> items = itemRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId());
        long subtotal = items.stream().mapToLong(i -> baseAmount(i.getUnitPriceCents(), i.getQuantity())).sum();
        long itemTax = items.stream().mapToLong(i -> safe(i.getTaxCents())).sum();
        long itemDiscount = items.stream().mapToLong(i -> safe(i.getDiscountCents())).sum();
        long orderDiscount = nonNegative(order.getDiscountCents());
        long approvedAmendments = amendmentRepository.findByTombstoneOrderIdOrderByAmendmentNoDesc(order.getId()).stream()
                .filter(amendment -> "APPROVED".equals(amendment.getStatus()))
                .mapToLong(amendment -> safe(amendment.getAmountDeltaCents()))
                .sum();
        long adjustedSubtotal = Math.max(0L, subtotal + approvedAmendments);
        long total = Math.max(0L, adjustedSubtotal + itemTax - itemDiscount - orderDiscount);
        order.setSubtotalCents(adjustedSubtotal);
        order.setTaxCents(itemTax);
        order.setDiscountCents(orderDiscount);
        order.setTotalCents(total);
        order.setBalanceCents(Math.max(0L, total - safe(order.getConfirmedFundingCents())));
        return order;
    }

    private TombstoneOrderEntity recalculateFunding(TombstoneOrderEntity order, String actor) {
        long confirmed = fundingRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .filter(a -> !Set.of("CANCELLED", "REJECTED").contains(a.getStatus()))
                .mapToLong(a -> safe(a.getConfirmedAmountCents())).sum();
        confirmed = Math.min(confirmed, safe(order.getTotalCents()));
        long balance = Math.max(0L, safe(order.getTotalCents()) - confirmed);
        String oldFunding = order.getFundingStatus();
        String oldOrder = order.getStatus();
        String fundingStatus = confirmed <= 0 ? "UNFUNDED" : (balance == 0 ? "FULLY_FUNDED" : "PARTIALLY_FUNDED");
        order.setConfirmedFundingCents(confirmed);
        order.setBalanceCents(balance);
        order.setFundingStatus(fundingStatus);
        if (!Set.of("CANCELLED", "COMPLETED", "IN_PRODUCTION", "READY_FOR_INSTALLATION", "INSTALLATION_SCHEDULED", "INSTALLED", "REWORK_REQUIRED").contains(order.getStatus())) {
            if (balance > 0) order.setStatus(confirmed > 0 ? "PARTIALLY_FUNDED" : "AWAITING_FUNDING");
            else if (assessmentRepository.findFirstByTombstoneOrderIdOrderByVersionNoDesc(order.getId()).isEmpty()) order.setStatus("SITE_ASSESSMENT_PENDING");
            else if (designRepository.findFirstByTombstoneOrderIdAndStatusOrderByVersionNoDesc(order.getId(), "APPROVED").isEmpty()) order.setStatus("DESIGN_PENDING");
            else order.setStatus("DESIGN_APPROVED");
        }
        order.setUpdatedBy(systemActor(actor));
        orderRepository.save(order);
        if (!Objects.equals(oldFunding, fundingStatus)) saveHistory(order.getId(), "FUNDING", oldFunding, fundingStatus, "Funding recalculated", actor);
        if (!Objects.equals(oldOrder, order.getStatus())) saveHistory(order.getId(), "ORDER", oldOrder, order.getStatus(), "Funding state changed", actor);
        return order;
    }

    private void replaceItems(TombstoneOrderEntity order, List<TombstoneDtos.ItemRequest> requests) {
        itemRepository.deleteByTombstoneOrderId(order.getId());
        for (TombstoneDtos.ItemRequest request : requests) {
            requireText(request.getDescription(), "item.description");
            BigDecimal qty = request.getQuantity() == null ? BigDecimal.ONE : request.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("item.quantity must be greater than zero");
            long unit = nonNegative(request.getUnitPriceCents());
            long discount = nonNegative(request.getDiscountCents());
            long tax = nonNegative(request.getTaxCents());
            long total = Math.max(0L, baseAmount(unit, qty) + tax - discount);
            itemRepository.save(TombstoneOrderItemEntity.builder()
                    .tombstoneOrderId(order.getId()).productId(trimToNull(request.getProductId()))
                    .itemType(firstNonBlank(normalizeOptional(request.getItemType()), "TOMBSTONE"))
                    .description(request.getDescription().trim()).material(trimToNull(request.getMaterial()))
                    .colour(trimToNull(request.getColour())).dimensions(trimToNull(request.getDimensions()))
                    .inscriptionText(trimToNull(request.getInscriptionText())).quantity(qty)
                    .unitPriceCents(unit).discountCents(discount).taxCents(tax).totalCents(total).build());
        }
    }

    private TombstoneOrderEntity createOrderInvoice(TombstoneOrderEntity order, String actor) {
        List<TombstoneOrderItemEntity> items = itemRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId());
        List<InvoiceLineEntity> lines = invoiceLines(items, List.of());
        InvoiceEntity invoice = InvoiceEntity.builder()
                .externalRef(order.getOrderNo()).sourceType("TOMBSTONE_ORDER").sourceId(order.getId())
                .partnerId(order.getCustomerPartnerId()).invoiceDate(LocalDate.now()).dueDate(LocalDate.now())
                .status("DRAFT").subtotalCents(order.getSubtotalCents()).taxCents(order.getTaxCents())
                .discountCents(order.getDiscountCents()).totalCents(order.getTotalCents())
                .paidCents(order.getConfirmedFundingCents()).balanceCents(order.getBalanceCents())
                .currency("ZAR").notes("Tombstone order " + order.getOrderNo()).createdBy(actor)
                .lines(new ArrayList<>(lines)).payments(new ArrayList<>()).build();
        invoice = invoiceService.createInvoice(invoice);
        order.setInvoiceId(invoice.getId());
        order.setUpdatedBy(actor);
        return orderRepository.save(order);
    }

    private void syncInvoice(TombstoneOrderEntity order, String actor) {
        if (!StringUtils.hasText(order.getInvoiceId())) return;
        invoiceRepository.findById(order.getInvoiceId()).ifPresent(invoice -> {
            List<TombstoneOrderItemEntity> items = itemRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId());
            List<TombstoneOrderAmendmentEntity> amendments = amendmentRepository.findByTombstoneOrderIdOrderByAmendmentNoDesc(order.getId()).stream()
                    .filter(a -> "APPROVED".equals(a.getStatus()) && safe(a.getAmountDeltaCents()) != 0).toList();
            invoice.getLines().clear();
            for (InvoiceLineEntity line : invoiceLines(items, amendments)) {
                line.setInvoice(invoice);
                invoice.getLines().add(line);
            }
            invoice.setPartnerId(order.getCustomerPartnerId());
            invoice.setSubtotalCents(order.getSubtotalCents());
            invoice.setTaxCents(order.getTaxCents());
            invoice.setDiscountCents(order.getDiscountCents());
            invoice.setTotalCents(order.getTotalCents());
            invoice.setPaidCents(order.getConfirmedFundingCents());
            invoice.setBalanceCents(order.getBalanceCents());
            invoice.setStatus(order.getBalanceCents() == 0 ? "PAID" : (order.getConfirmedFundingCents() > 0 ? "PARTIALLY_PAID" : "DRAFT"));
            invoice.setUpdatedAt(LocalDateTime.now());
            invoice.setUpdatedBy(actor);
            invoiceRepository.save(invoice);
        });
    }

    private List<InvoiceLineEntity> invoiceLines(List<TombstoneOrderItemEntity> items, List<TombstoneOrderAmendmentEntity> amendments) {
        List<InvoiceLineEntity> lines = new ArrayList<>();
        for (TombstoneOrderItemEntity item : items) {
            lines.add(InvoiceLineEntity.builder().productId(item.getProductId()).description(item.getDescription())
                    .quantity(item.getQuantity().doubleValue()).unitPriceCents(item.getUnitPriceCents())
                    .discountCents(item.getDiscountCents()).taxCents(item.getTaxCents())
                    .subtotalCents(baseAmount(item.getUnitPriceCents(), item.getQuantity())).totalCents(item.getTotalCents()).build());
        }
        for (TombstoneOrderAmendmentEntity amendment : amendments) {
            lines.add(InvoiceLineEntity.builder().description("Order amendment: " + amendment.getReason())
                    .quantity(1.0).unitPriceCents(amendment.getAmountDeltaCents()).discountCents(0L).taxCents(0L)
                    .subtotalCents(amendment.getAmountDeltaCents()).totalCents(amendment.getAmountDeltaCents()).build());
        }
        return lines;
    }

    private void generateInstallments(TombstoneLaybyAgreementEntity agreement) {
        long remaining = safe(agreement.getTotalAmountCents());
        int no = 1;
        LocalDate due = agreement.getStartDate();
        long deposit = Math.min(remaining, safe(agreement.getDepositRequiredCents()));
        if (deposit > 0) {
            installmentRepository.save(installment(agreement.getId(), no++, due, deposit));
            remaining -= deposit;
            due = nextDueDate(due, agreement.getPaymentFrequency());
        }
        while (remaining > 0 && no <= 120) {
            long amount = Math.min(remaining, safe(agreement.getInstallmentAmountCents()));
            installmentRepository.save(installment(agreement.getId(), no++, due, amount));
            remaining -= amount;
            due = nextDueDate(due, agreement.getPaymentFrequency());
        }
        if (remaining > 0) throw new IllegalStateException("Lay-by schedule exceeds 120 installments; increase installmentAmountCents");
        if (agreement.getExpectedSettlementDate() == null) {
            agreement.setExpectedSettlementDate(due);
            laybyRepository.save(agreement);
        }
    }

    private TombstoneLaybyInstallmentEntity installment(String agreementId, int no, LocalDate due, long amount) {
        return TombstoneLaybyInstallmentEntity.builder().laybyAgreementId(agreementId).installmentNo(no)
                .dueDate(due).amountCents(amount).paidAmountCents(0L).status("SCHEDULED").build();
    }

    private void applyPaymentToInstallments(TombstoneLaybyAgreementEntity agreement, String receiptId, long amount) {
        long remaining = amount;
        for (TombstoneLaybyInstallmentEntity installment : installmentRepository
                .findByLaybyAgreementIdOrderByInstallmentNoAsc(agreement.getId())) {
            if (remaining <= 0) break;
            long installmentBalance = Math.max(0L, safe(installment.getAmountCents()) - safe(installment.getPaidAmountCents()));
            if (installmentBalance == 0) continue;
            long applied = Math.min(remaining, installmentBalance);
            installment.setPaidAmountCents(safe(installment.getPaidAmountCents()) + applied);
            installment.setReceiptId(receiptId);
            installment.setPaidAt(LocalDateTime.now());
            installment.setStatus(installment.getPaidAmountCents() >= installment.getAmountCents() ? "PAID" : "PARTIALLY_PAID");
            installmentRepository.save(installment);
            remaining -= applied;
        }
    }

    private TombstoneOrderAmendmentEntity createAmendmentInternal(String orderId, String reason, Long delta,
                                                                   String attachmentId, String actor) {
        getOrderEntity(orderId);
        int no = amendmentRepository.findByTombstoneOrderIdOrderByAmendmentNoDesc(orderId).stream()
                .map(TombstoneOrderAmendmentEntity::getAmendmentNo).max(Integer::compareTo).orElse(0) + 1;
        return amendmentRepository.save(TombstoneOrderAmendmentEntity.builder()
                .tombstoneOrderId(orderId).amendmentNo(no).reason(reason.trim())
                .amountDeltaCents(delta == null ? 0L : delta).status("PENDING_CUSTOMER_APPROVAL")
                .supportingAttachmentId(trimToNull(attachmentId)).requestedAt(LocalDateTime.now())
                .requestedBy(systemActor(actor)).build());
    }

    private void replaceInstallationTeam(String installationId, List<TombstoneDtos.TeamMemberRequest> requests) {
        teamRepository.deleteByInstallationId(installationId);
        if (requests == null) return;
        for (TombstoneDtos.TeamMemberRequest request : requests) {
            requireText(request.getEmployeePartnerId(), "team.employeePartnerId");
            teamRepository.save(TombstoneInstallationTeamEntity.builder().installationId(installationId)
                    .employeePartnerId(request.getEmployeePartnerId().trim()).teamRole(trimToNull(request.getTeamRole())).build());
        }
    }

    private void replaceInstallationMaterials(String installationId, List<TombstoneDtos.InstallationMaterialRequest> requests) {
        materialRepository.deleteByInstallationId(installationId);
        if (requests == null) return;
        for (TombstoneDtos.InstallationMaterialRequest request : requests) {
            requireText(request.getDescription(), "material.description");
            BigDecimal qty = request.getQuantity() == null ? BigDecimal.ONE : request.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("material.quantity must be greater than zero");
            }
            materialRepository.save(TombstoneInstallationMaterialEntity.builder().installationId(installationId)
                    .productId(trimToNull(request.getProductId())).description(request.getDescription().trim())
                    .quantity(qty).uom(trimToNull(request.getUom())).consumedQuantity(BigDecimal.ZERO).build());
        }
    }

    private void createDefaultChecklist(String installationId) {
        for (String[] definition : DEFAULT_CHECKLIST) {
            checklistRepository.save(TombstoneInstallationChecklistEntity.builder()
                    .installationId(installationId).checklistCode(definition[0]).checklistLabel(definition[1])
                    .required(true).completed(false).build());
        }
    }

    private void copyInstallationResources(String sourceInstallationId, String targetInstallationId) {
        List<TombstoneDtos.TeamMemberRequest> team = teamRepository.findByInstallationIdOrderByCreatedAtAsc(sourceInstallationId).stream()
                .map(t -> TombstoneDtos.TeamMemberRequest.builder().employeePartnerId(t.getEmployeePartnerId()).teamRole(t.getTeamRole()).build()).toList();
        List<TombstoneDtos.InstallationMaterialRequest> materials = materialRepository.findByInstallationIdOrderByCreatedAtAsc(sourceInstallationId).stream()
                .map(m -> TombstoneDtos.InstallationMaterialRequest.builder().productId(m.getProductId()).description(m.getDescription())
                        .quantity(m.getQuantity()).uom(m.getUom()).build()).toList();
        replaceInstallationTeam(targetInstallationId, team);
        replaceInstallationMaterials(targetInstallationId, materials);
    }

    private TombstoneProductionJobEntity resolveReadyProductionJob(String orderId, String jobId) {
        TombstoneProductionJobEntity job = StringUtils.hasText(jobId)
                ? productionRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Production job not found: " + jobId))
                : productionRepository.findFirstByTombstoneOrderIdOrderByCreatedAtDesc(orderId)
                    .orElseThrow(() -> new IllegalStateException("A production job is required before installation"));
        if (!orderId.equals(job.getTombstoneOrderId()) || !"READY_FOR_INSTALLATION".equals(job.getStatus())) {
            throw new IllegalStateException("The production job must pass quality check and be ready for installation");
        }
        return job;
    }

    private TombstoneSiteAssessmentEntity requireCompletedAssessment(String orderId) {
        return assessmentRepository.findByTombstoneOrderIdOrderByVersionNoDesc(orderId).stream()
                .filter(a -> "COMPLETED".equals(a.getStatus())).findFirst()
                .orElseThrow(() -> new IllegalStateException("A completed site assessment is required"));
    }

    private void requireFullyFunded(TombstoneOrderEntity order) {
        if (!"FULLY_FUNDED".equals(order.getFundingStatus()) || safe(order.getBalanceCents()) > 0) {
            throw new IllegalStateException("The tombstone order must be fully funded before this operation");
        }
    }

    private ReceiptEntity getPostedReceipt(String receiptId) {
        ReceiptEntity receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));
        if (receipt.getStatus() != ReceiptStatus.POSTED) throw new IllegalStateException("Only POSTED receipts can fund a tombstone order");
        return receipt;
    }

    private void assertReceiptAvailable(ReceiptEntity receipt, long requested) {
        long allocated = receiptAllocationRepository.findByReceiptId(receipt.getId()).stream()
                .filter(a -> a.getStatus() == ReceiptStatus.POSTED)
                .mapToLong(a -> safe(a.getAmountCents())).sum();
        long available = Math.max(0L, safe(receipt.getTotalAmountCents()) - allocated);
        if (requested > available) throw new IllegalArgumentException("Receipt has only " + available + " cents available for allocation");
    }

    private TombstoneDtos.OrderResponse toResponse(TombstoneOrderEntity order, boolean aggregate) {
        TombstoneDtos.OrderResponse response = TombstoneDtos.OrderResponse.builder()
                .id(order.getId()).orderNo(order.getOrderNo()).customerPartnerId(order.getCustomerPartnerId())
                .membershipId(order.getMembershipId()).deceasedPartnerId(order.getDeceasedPartnerId())
                .deceasedName(order.getDeceasedName()).funeralServiceId(order.getFuneralServiceId())
                .cemeteryName(order.getCemeteryName()).cemeteryArea(order.getCemeteryArea()).graveNumber(order.getGraveNumber())
                .graveLatitude(order.getGraveLatitude()).graveLongitude(order.getGraveLongitude())
                .salesArea(order.getSalesArea()).workcenterId(order.getWorkcenterId())
                .expectedInstallationDate(order.getExpectedInstallationDate()).fundingMethod(order.getFundingMethod())
                .status(order.getStatus()).fundingStatus(order.getFundingStatus()).productionStatus(order.getProductionStatus())
                .installationStatus(order.getInstallationStatus()).subtotalCents(order.getSubtotalCents())
                .taxCents(order.getTaxCents()).discountCents(order.getDiscountCents()).totalCents(order.getTotalCents())
                .confirmedFundingCents(order.getConfirmedFundingCents()).balanceCents(order.getBalanceCents())
                .invoiceId(order.getInvoiceId()).notes(order.getNotes()).cancellationReason(order.getCancellationReason())
                .cancelledAt(order.getCancelledAt()).createdAt(order.getCreatedAt()).createdBy(order.getCreatedBy())
                .updatedAt(order.getUpdatedAt()).updatedBy(order.getUpdatedBy()).build();
        if (!aggregate) return response;
        response.setItems(itemRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId()).stream().map(this::itemMap).toList());
        response.setFundingAllocations(fundingRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId()).stream().map(this::fundingMap).toList());
        laybyRepository.findByTombstoneOrderId(order.getId()).ifPresent(agreement -> {
            response.setLaybyAgreement(laybyMap(agreement));
            response.setLaybyInstallments(installmentRepository.findByLaybyAgreementIdOrderByInstallmentNoAsc(agreement.getId()).stream().map(this::installmentMap).toList());
        });
        response.setAssessments(assessmentRepository.findByTombstoneOrderIdOrderByVersionNoDesc(order.getId()).stream().map(this::assessmentMap).toList());
        response.setAmendments(amendmentRepository.findByTombstoneOrderIdOrderByAmendmentNoDesc(order.getId()).stream().map(this::amendmentMap).toList());
        response.setDesigns(designRepository.findByTombstoneOrderIdOrderByVersionNoDesc(order.getId()).stream().map(this::designMap).toList());
        response.setProductionJobs(productionRepository.findByTombstoneOrderIdOrderByCreatedAtDesc(order.getId()).stream().map(this::productionMap).toList());
        response.setInstallations(installationRepository.findByTombstoneOrderIdOrderByCreatedAtDesc(order.getId()).stream().map(this::installationMap).toList());
        response.setStatusHistory(historyRepository.findByTombstoneOrderIdOrderByChangedAtDesc(order.getId()).stream().map(this::historyMap).toList());
        return response;
    }

    private Map<String, Object> itemMap(TombstoneOrderItemEntity e) { return map(
            "id", e.getId(), "productId", e.getProductId(), "itemType", e.getItemType(), "description", e.getDescription(),
            "material", e.getMaterial(), "colour", e.getColour(), "dimensions", e.getDimensions(), "inscriptionText", e.getInscriptionText(),
            "quantity", e.getQuantity(), "unitPriceCents", e.getUnitPriceCents(), "discountCents", e.getDiscountCents(),
            "taxCents", e.getTaxCents(), "totalCents", e.getTotalCents()); }
    private Map<String, Object> fundingMap(TombstoneFundingAllocationEntity e) { return map(
            "id", e.getId(), "fundingType", e.getFundingType(), "sourceType", e.getSourceType(), "sourceId", e.getSourceId(),
            "sourceNo", e.getSourceNo(), "allocatedAmountCents", e.getAllocatedAmountCents(), "confirmedAmountCents", e.getConfirmedAmountCents(),
            "status", e.getStatus(), "confirmedAt", e.getConfirmedAt(), "notes", e.getNotes(), "createdAt", e.getCreatedAt()); }
    private Map<String, Object> laybyMap(TombstoneLaybyAgreementEntity e) { return map(
            "id", e.getId(), "agreementNo", e.getAgreementNo(), "tombstoneOrderId", e.getTombstoneOrderId(),
            "depositRequiredCents", e.getDepositRequiredCents(), "installmentAmountCents", e.getInstallmentAmountCents(),
            "paymentFrequency", e.getPaymentFrequency(), "startDate", e.getStartDate(), "expectedSettlementDate", e.getExpectedSettlementDate(),
            "gracePeriodDays", e.getGracePeriodDays(), "administrationFeeCents", e.getAdministrationFeeCents(),
            "totalAmountCents", e.getTotalAmountCents(), "paidAmountCents", e.getPaidAmountCents(), "balanceCents", e.getBalanceCents(),
            "status", e.getStatus(), "createdAt", e.getCreatedAt()); }
    private Map<String, Object> installmentMap(TombstoneLaybyInstallmentEntity e) { return map(
            "id", e.getId(), "installmentNo", e.getInstallmentNo(), "dueDate", e.getDueDate(), "amountCents", e.getAmountCents(),
            "paidAmountCents", e.getPaidAmountCents(), "receiptId", e.getReceiptId(), "paidAt", e.getPaidAt(), "status", e.getStatus()); }
    private Map<String, Object> assessmentMap(TombstoneSiteAssessmentEntity e) { return map(
            "id", e.getId(), "tombstoneOrderId", e.getTombstoneOrderId(), "versionNo", e.getVersionNo(), "status", e.getStatus(),
            "scheduledAt", e.getScheduledAt(), "assessedAt", e.getAssessedAt(), "assessorPartnerId", e.getAssessorPartnerId(),
            "cemeteryName", e.getCemeteryName(), "graveNumber", e.getGraveNumber(), "graveLatitude", e.getGraveLatitude(),
            "graveLongitude", e.getGraveLongitude(), "graveLengthMm", e.getGraveLengthMm(), "graveWidthMm", e.getGraveWidthMm(),
            "foundationCondition", e.getFoundationCondition(), "accessRestrictions", e.getAccessRestrictions(), "cemeteryRules", e.getCemeteryRules(),
            "permitRequired", e.getPermitRequired(), "permitReference", e.getPermitReference(), "permitApproved", e.getPermitApproved(),
            "travelDistanceKm", e.getTravelDistanceKm(), "additionalWorkRequired", e.getAdditionalWorkRequired(),
            "additionalCostCents", e.getAdditionalCostCents(), "photoAttachmentIds", fromJsonList(e.getPhotoAttachmentIdsJson()),
            "failureReason", e.getFailureReason(), "createdAt", e.getCreatedAt()); }
    private Map<String, Object> amendmentMap(TombstoneOrderAmendmentEntity e) { return map(
            "id", e.getId(), "tombstoneOrderId", e.getTombstoneOrderId(), "amendmentNo", e.getAmendmentNo(), "reason", e.getReason(),
            "amountDeltaCents", e.getAmountDeltaCents(), "status", e.getStatus(), "supportingAttachmentId", e.getSupportingAttachmentId(),
            "requestedAt", e.getRequestedAt(), "requestedBy", e.getRequestedBy(), "approvedAt", e.getApprovedAt(), "approvedBy", e.getApprovedBy(),
            "rejectedAt", e.getRejectedAt(), "rejectedBy", e.getRejectedBy(), "responseNotes", e.getResponseNotes()); }
    private Map<String, Object> designMap(TombstoneDesignEntity e) { return map(
            "id", e.getId(), "tombstoneOrderId", e.getTombstoneOrderId(), "versionNo", e.getVersionNo(), "status", e.getStatus(),
            "inscriptionText", e.getInscriptionText(), "fontName", e.getFontName(), "layoutNotes", e.getLayoutNotes(),
            "symbols", fromJsonList(e.getSymbolsJson()), "material", e.getMaterial(), "colour", e.getColour(), "dimensions", e.getDimensions(),
            "designAttachmentId", e.getDesignAttachmentId(), "sentForApprovalAt", e.getSentForApprovalAt(),
            "customerApprovalMethod", e.getCustomerApprovalMethod(), "customerApprovalReference", e.getCustomerApprovalReference(),
            "approvedAt", e.getApprovedAt(), "approvedBy", e.getApprovedBy(), "changeRequest", e.getChangeRequest(), "createdAt", e.getCreatedAt()); }
    private Map<String, Object> productionMap(TombstoneProductionJobEntity e) { return map(
            "id", e.getId(), "jobNo", e.getJobNo(), "tombstoneOrderId", e.getTombstoneOrderId(), "designId", e.getDesignId(),
            "internalProduction", e.getInternalProduction(), "supplierPartnerId", e.getSupplierPartnerId(), "purchaseOrderId", e.getPurchaseOrderId(),
            "status", e.getStatus(), "plannedStartDate", e.getPlannedStartDate(), "plannedCompletionDate", e.getPlannedCompletionDate(),
            "actualStartAt", e.getActualStartAt(), "actualCompletionAt", e.getActualCompletionAt(),
            "qualityCheckedAt", e.getQualityCheckedAt(), "qualityCheckedBy", e.getQualityCheckedBy(), "qualityNotes", e.getQualityNotes(),
            "createdAt", e.getCreatedAt()); }
    private Map<String, Object> installationMap(TombstoneInstallationEntity e) {
        Map<String, Object> result = map(
                "id", e.getId(), "installationNo", e.getInstallationNo(), "tombstoneOrderId", e.getTombstoneOrderId(),
                "productionJobId", e.getProductionJobId(), "reworkOfInstallationId", e.getReworkOfInstallationId(), "status", e.getStatus(),
                "scheduledStartAt", e.getScheduledStartAt(), "scheduledEndAt", e.getScheduledEndAt(), "dispatchedAt", e.getDispatchedAt(),
                "arrivedAt", e.getArrivedAt(), "installedAt", e.getInstalledAt(), "acceptedAt", e.getAcceptedAt(), "completedAt", e.getCompletedAt(),
                "cemeteryName", e.getCemeteryName(), "graveNumber", e.getGraveNumber(), "assignedVehicleId", e.getAssignedVehicleId(),
                "contactPerson", e.getContactPerson(), "contactNumber", e.getContactNumber(), "permitReference", e.getPermitReference(),
                "instructions", e.getInstructions(), "beforePhotoAttachmentIds", fromJsonList(e.getBeforePhotoAttachmentIdsJson()),
                "afterPhotoAttachmentIds", fromJsonList(e.getAfterPhotoAttachmentIdsJson()), "customerRepresentativeName", e.getCustomerRepresentativeName(),
                "customerSignatureAttachmentId", e.getCustomerSignatureAttachmentId(), "installerSignatureAttachmentId", e.getInstallerSignatureAttachmentId(),
                "completionNotes", e.getCompletionNotes(), "reworkReason", e.getReworkReason(), "createdAt", e.getCreatedAt());
        result.put("team", teamRepository.findByInstallationIdOrderByCreatedAtAsc(e.getId()).stream().map(t -> map(
                "id", t.getId(), "employeePartnerId", t.getEmployeePartnerId(), "teamRole", t.getTeamRole())).toList());
        result.put("materials", materialRepository.findByInstallationIdOrderByCreatedAtAsc(e.getId()).stream().map(m -> map(
                "id", m.getId(), "productId", m.getProductId(), "description", m.getDescription(), "quantity", m.getQuantity(),
                "uom", m.getUom(), "consumedQuantity", m.getConsumedQuantity())).toList());
        result.put("checklist", checklistRepository.findByInstallationIdOrderByCreatedAtAsc(e.getId()).stream().map(c -> map(
                "id", c.getId(), "checklistCode", c.getChecklistCode(), "checklistLabel", c.getChecklistLabel(), "required", c.getRequired(),
                "completed", c.getCompleted(), "completedAt", c.getCompletedAt(), "completedBy", c.getCompletedBy(),
                "notes", c.getNotes(), "evidenceAttachmentId", c.getEvidenceAttachmentId())).toList());
        return result;
    }
    private Map<String, Object> historyMap(TombstoneStatusHistoryEntity e) { return map(
            "id", e.getId(), "statusDimension", e.getStatusDimension(), "fromStatus", e.getFromStatus(), "toStatus", e.getToStatus(),
            "reason", e.getReason(), "changedAt", e.getChangedAt(), "changedBy", e.getChangedBy()); }

    private Map<String, Object> cancellationSummary(TombstoneOrderEntity order) {
        Map<String, Long> refundByType = fundingRepository.findByTombstoneOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .filter(a -> safe(a.getConfirmedAmountCents()) > 0)
                .collect(Collectors.groupingBy(TombstoneFundingAllocationEntity::getFundingType,
                        LinkedHashMap::new, Collectors.summingLong(a -> safe(a.getConfirmedAmountCents()))));
        return map("orderId", order.getId(), "orderNo", order.getOrderNo(), "status", order.getStatus(),
                "refundRequiredCents", refundByType.values().stream().mapToLong(Long::longValue).sum(),
                "refundByFundingType", refundByType, "reason", order.getCancellationReason());
    }

    private TombstoneOrderEntity getOrderEntity(String id) {
        return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tombstone order not found: " + id));
    }
    private TombstoneInstallationEntity getInstallationEntity(String id) {
        return installationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tombstone installation not found: " + id));
    }
    private void assertOrderEditable(TombstoneOrderEntity order) {
        if (Set.of("IN_PRODUCTION", "READY_FOR_INSTALLATION", "INSTALLATION_SCHEDULED", "INSTALLED", "COMPLETED", "CANCELLED").contains(order.getStatus())) {
            throw new IllegalStateException("The tombstone order can no longer be edited in status " + order.getStatus());
        }
        if (designRepository.findFirstByTombstoneOrderIdAndStatusOrderByVersionNoDesc(order.getId(), "APPROVED").isPresent()) {
            throw new IllegalStateException("Create an order amendment after a design has been approved");
        }
    }
    private void assertNotClosed(TombstoneOrderEntity order) {
        if (Set.of("COMPLETED", "CANCELLED").contains(order.getStatus())) throw new IllegalStateException("The tombstone order is " + order.getStatus());
    }
    private void saveHistory(String orderId, String dimension, String from, String to, String reason, String actor) {
        historyRepository.save(TombstoneStatusHistoryEntity.builder().tombstoneOrderId(orderId).statusDimension(dimension)
                .fromStatus(from).toStatus(to).reason(trimToNull(reason)).changedAt(LocalDateTime.now()).changedBy(systemActor(actor)).build());
    }

    private static long count(List<TombstoneOrderEntity> orders, java.util.function.Predicate<TombstoneOrderEntity> predicate) {
        return orders.stream().filter(predicate).count();
    }
    private static long safe(Long value) { return value == null ? 0L : value; }
    private static long nonNegative(Long value) { return Math.max(0L, safe(value)); }
    private static long positive(Long value, String field) {
        if (value == null || value <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return value;
    }
    private static long baseAmount(Long unitPrice, BigDecimal quantity) {
        return BigDecimal.valueOf(nonNegative(unitPrice)).multiply(quantity == null ? BigDecimal.ONE : quantity)
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
    private static LocalDate nextDueDate(LocalDate date, String frequency) {
        return switch (frequency) { case "WEEKLY" -> date.plusWeeks(1); case "FORTNIGHTLY" -> date.plusWeeks(2); default -> date.plusMonths(1); };
    }
    private static String formatNumber(String prefix, String number) { return prefix + "-" + String.format("%010d", Long.parseLong(number)); }
    private static String normalizeRequired(String value, String field) {
        requireText(value, field);
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
    private static String normalizeOptional(String value) {
        return !StringUtils.hasText(value) ? null : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
    private static String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private static String systemActor(String actor) { return StringUtils.hasText(actor) ? actor.trim() : "SYSTEM"; }
    private static String firstNonBlank(String first, String second) { return StringUtils.hasText(first) ? first.trim() : trimToNull(second); }
    private static String joinNotes(String first, String second) {
        if (!StringUtils.hasText(first)) return trimToNull(second);
        if (!StringUtils.hasText(second)) return first;
        return first + "\n" + second.trim();
    }
    private static boolean containsIgnoreCase(String value, String q) { return value != null && value.toLowerCase(Locale.ROOT).contains(q); }
    private static void requireText(String value, String field) { if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required"); }
    private static void require(Object value, String message) { if (value == null) throw new IllegalArgumentException(message); }
    private String toJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Unable to serialise tombstone data", e); }
    }
    private List<String> fromJsonList(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)); }
        catch (Exception ignored) { return List.of(); }
    }
    private static Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) map.put(String.valueOf(values[i]), values[i + 1]);
        return map;
    }
}
