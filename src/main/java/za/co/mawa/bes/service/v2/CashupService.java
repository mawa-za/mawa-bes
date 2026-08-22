package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.payapp.*;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.entity.v2.ManualPremiumReceiptEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.repository.v2.ManualPremiumReceiptRepository;
import za.co.mawa.bes.service.AttachmentService;

import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service(value = "CashupServiceV2")
@RequiredArgsConstructor
public class CashupService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_AWAITING_DEPOSITS = "AWAITING_DEPOSITS";
    private static final String STATUS_COMPLETED = "COMPLETED"; // Legacy device-completed status
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String SOURCE_MANUAL_RECEIPT_BOOK = "MANUAL_RECEIPT_BOOK";
    private static final String SOURCE_ERP_ONLINE_EFT = "ERP_ONLINE_EFT";
    private static final String SOURCE_MAWA_PAY_EFT = "MAWA_PAY_EFT";

    private final CashupRepository cashupRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final CashupReceiptRepository cashupReceiptRepository;
    private final CashupDepositRepository cashupDepositRepository;
    private final ManualPremiumReceiptRepository manualPremiumReceiptRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final AttachmentService attachmentService;
    private final NumberAllocationService numberAllocationService;
    private final ApprovalService approvalService;
    private final ReferenceDataValidationService referenceDataValidationService;
    private final ManualReceiptBookService manualReceiptBookService;
    private final ObjectMapper objectMapper;

    /**
     * Upserts the cashup received from the offline Flutter app.
     *
     * New MawaPay flow:
     * 1. Device always has an active/open cashup.
     * 2. Every CASH/CARD receipt is attached to the active cashup immediately; EFT uses its own cashup.
     * 3. The app keeps syncing the same cashup while it is OPEN.
     * 4. When the cashier closes the cashup on the device, the same cashup is synced as AWAITING_DEPOSITS.
     *
     * Because of this, duplicate cashupNo must NOT be treated as an error. It is the normal
     * continuous-sync path and should update the existing portal cashup.
     */
    @Transactional
    public CashupResponse submitCashup(CashupRequest request) {
        validateRequest(request);

        CashupEntity cashup = cashupRepository.findByCashupNo(request.getCashupNo())
                .orElseGet(CashupEntity::new);

        boolean created = cashup.getId() == null;
        boolean mawaPayEft = isMawaPayIndividualEftCashup(request);
        String requestedStatus = resolveStatus(request, mawaPayEft);

        if (!created && isLocked(cashup)) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is " + cashup.getStatus() + " and cannot be updated from device sync")
                    .build();
        }

        boolean recoverableElectronicAwaitingDeposits = mawaPayEft
                && STATUS_AWAITING_DEPOSITS.equalsIgnoreCase(cashup.getStatus())
                && clean(cashup.getApprovalRequestId()) == null;
        if (!created && isClosedForDeviceSync(cashup) && STATUS_OPEN.equals(requestedStatus)
                && !recoverableElectronicAwaitingDeposits) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is already awaiting deposits/submitted; stale OPEN device sync ignored")
                    .build();
        }

        applyRequestToCashup(cashup, request, requestedStatus, created);
        if (mawaPayEft) {
            cashup.setSource(SOURCE_MAWA_PAY_EFT);
            cashup.setDepositTotalCents(0L);
            cashup.setDepositCount(0);
        }
        cashup = cashupRepository.save(cashup);

        replacePaymentSummaries(cashup, request.getAmountByMethod(), request.getCountByMethod());
        replaceReceipts(cashup, request.getReceipts());

        if (mawaPayEft) {
            CashupSubmitForApprovalRequest submitRequest = new CashupSubmitForApprovalRequest();
            submitRequest.setRequesterId(request.getUserId());
            submitRequest.setComments("Automatically submitted after the MawaPay "
                    + eftPaymentMethod(request)
                    + " payment was processed. Deposit not required.");
            return submitForApproval(cashup.getId(), submitRequest);
        }

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message(created ? "Cashup created successfully" : "Cashup updated successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public CashupSummaryResponse getCashup(String id) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        List<CashupPaymentSummaryDto> payments = cashupPaymentSummaryRepository
                .findByCashupId(cashup.getId())
                .stream()
                .map(item -> CashupPaymentSummaryDto.builder()
                        .paymentMethod(item.getPaymentMethod())
                        .amountCents(item.getAmountCents())
                        .paymentCount(item.getPaymentCount())
                        .build())
                .toList();

        return CashupSummaryResponse.builder()
                .id(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .deviceId(cashup.getDeviceId())
                .userId(cashup.getUserId())
                .cashierName(resolveCashierName(cashup.getUserId()))
                .cashupDate(cashup.getCashupDate())
                .totalCents(cashup.getTotalCents())
                .receiptCount(cashup.getReceiptCount())
                .status(cashup.getStatus())
                .source(cashup.getSource())
                .receiptBookNo(cashup.getReceiptBookNo())
                .receiptFromNo(cashup.getReceiptFromNo())
                .receiptToNo(cashup.getReceiptToNo())
                .manualAmountCents(cashup.getManualAmountCents())
                .receiptTotalCents(cashup.getReceiptTotalCents())
                .varianceCents(cashup.getVarianceCents())
                .employeeResponsibleId(cashup.getEmployeeResponsibleId())
                .employeeResponsibleName(cashup.getEmployeeResponsibleName())
                .areaCode(cashup.getAreaCode())
                .areaName(cashup.getAreaName())
                .depositTotalCents(defaultLong(cashup.getDepositTotalCents()))
                .depositCount(defaultInt(cashup.getDepositCount()))
                .approvalRequestId(cashup.getApprovalRequestId())
                .payments(payments)
                .deposits(getDeposits(cashup.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<CashupSummaryResponse> getActiveCashup(String deviceId, String userId) {
        return cashupRepository
                .findFirstByDeviceIdAndUserIdAndStatusOrderByCreatedAtDesc(deviceId, userId, STATUS_OPEN)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getCashupsByDevice(String deviceId) {
        return cashupRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getCashupsByUserAndDateRange(
            String userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return cashupRepository
                .findByUserIdAndCashupDateBetweenOrderByCashupDateDesc(userId, fromDate, toDate)
                .stream()
                .map(this::toSummary)
                .toList();
    }


    @Transactional(readOnly = true)
    public Slice<CashupListItemResponse> getPage(String status, String search, Pageable pageable) {
        final String normalizedStatus = clean(status);
        final String normalizedSearch = clean(search);
        Slice<CashupEntity> page = normalizedSearch != null
                ? cashupRepository.search(normalizedStatus, normalizedSearch, pageable)
                : normalizedStatus == null || "ALL".equalsIgnoreCase(normalizedStatus)
                    ? cashupRepository.findAllByOrderByCashupDateDescCreatedAtDesc(pageable)
                    : cashupRepository.findByStatusIgnoreCaseOrderByCashupDateDescCreatedAtDesc(normalizedStatus, pageable);
        Map<String, String> cashierNames = resolveCashierNames(page.getContent());
        return page.map(cashup -> toListItem(cashup, cashierNames.get(cashup.getUserId())));
    }

    private CashupListItemResponse toListItem(CashupEntity cashup, String cashierName) {
        return CashupListItemResponse.builder()
                .id(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .deviceId(cashup.getDeviceId())
                .userId(cashup.getUserId())
                .cashierName(cashierName)
                .cashupDate(cashup.getCashupDate())
                .totalCents(defaultLong(cashup.getTotalCents()))
                .receiptCount(defaultInt(cashup.getReceiptCount()))
                .status(cashup.getStatus())
                .source(cashup.getSource())
                .receiptBookNo(cashup.getReceiptBookNo())
                .receiptFromNo(cashup.getReceiptFromNo())
                .receiptToNo(cashup.getReceiptToNo())
                .manualAmountCents(cashup.getManualAmountCents())
                .receiptTotalCents(cashup.getReceiptTotalCents())
                .varianceCents(cashup.getVarianceCents())
                .employeeResponsibleId(cashup.getEmployeeResponsibleId())
                .employeeResponsibleName(cashup.getEmployeeResponsibleName())
                .areaCode(cashup.getAreaCode())
                .areaName(cashup.getAreaName())
                .depositTotalCents(defaultLong(cashup.getDepositTotalCents()))
                .depositCount(defaultInt(cashup.getDepositCount()))
                .createdAt(cashup.getCreatedAt())
                .updatedAt(cashup.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getAll() {
        return cashupRepository
                .findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public CashupSummaryResponse createManualCashup(ManualCashupCreateRequest request) {
        validateManualCashupRequest(request);

        var receiptBook = manualReceiptBookService.requireActiveBookForRange(
                request.getReceiptFromNo(), request.getReceiptToNo());
        var bookUsage = manualReceiptBookService.validateBookUsage(
                receiptBook, request.getEmployeeResponsibleId(), request.getAreaCode());
        String receiptBookNo = receiptBook.getReceiptBookNo();
        BigInteger fromNo = parseManualReceiptNumber(request.getReceiptFromNo(), "receiptFromNo");
        BigInteger toNo = parseManualReceiptNumber(request.getReceiptToNo(), "receiptToNo");
        if (fromNo.compareTo(toNo) > 0) {
            throw new IllegalArgumentException("receiptFromNo cannot be greater than receiptToNo");
        }

        preventOverlappingManualCashup(receiptBookNo, fromNo, toNo);

        List<ManualPremiumReceiptEntity> selectedReceipts = manualPremiumReceiptRepository
                .findByReceiptBookNoForUpdate(receiptBookNo)
                .stream()
                .filter(receipt -> {
                    BigInteger number = parseManualReceiptNumber(receipt.getManualReceiptNo(), "manualReceiptNo");
                    return number.compareTo(fromNo) >= 0 && number.compareTo(toNo) <= 0;
                })
                .sorted((left, right) -> parseManualReceiptNumber(left.getManualReceiptNo(), "manualReceiptNo")
                        .compareTo(parseManualReceiptNumber(right.getManualReceiptNo(), "manualReceiptNo")))
                .toList();

        // A manual cashup is a physical receipt-book declaration. Captured manual receipt
        // rows are linked when they exist, but they are not a prerequisite for creating the
        // cashup because the operator supplies the authoritative manual amount and range.
        List<String> alreadyCashupped = selectedReceipts.stream()
                .filter(receipt -> clean(receipt.getCashupId()) != null)
                .map(ManualPremiumReceiptEntity::getManualReceiptNo)
                .toList();
        if (!alreadyCashupped.isEmpty()) {
            throw new IllegalStateException("The following manual receipts already belong to a cashup: "
                    + String.join(", ", alreadyCashupped));
        }

        long receiptTotalCents = selectedReceipts.stream()
                .mapToLong(receipt -> defaultLong(receipt.getAmountCents()))
                .sum();
        long totalCents = defaultLong(request.getAmountCents());
        int declaredReceiptCount = manualReceiptRangeCount(fromNo, toNo);
        boolean completeCapturedRange = selectedReceipts.size() == declaredReceiptCount;

        CashupEntity cashup = new CashupEntity();
        cashup.setCashupNo(Long.parseLong(numberAllocationService.allocateNumber("CASHUP")));
        cashup.setDeviceId("ERP-MANUAL-" + receiptBookNo);
        cashup.setUserId(request.getUserId().trim());
        cashup.setCashupDate(clean(request.getCashupDate()) == null
                ? LocalDate.now()
                : parseDate(request.getCashupDate()));
        cashup.setTotalCents(totalCents);
        cashup.setReceiptCount(declaredReceiptCount);
        cashup.setStatus(STATUS_AWAITING_DEPOSITS);
        cashup.setSource(SOURCE_MANUAL_RECEIPT_BOOK);
        cashup.setReceiptBookNo(receiptBookNo);
        cashup.setReceiptFromNo(request.getReceiptFromNo().trim());
        cashup.setReceiptToNo(request.getReceiptToNo().trim());
        cashup.setManualAmountCents(totalCents);
        // The manual amount remains authoritative until every receipt number in the declared
        // range has a captured system row. Partial capture must not create a false variance.
        long effectiveReceiptTotalCents = completeCapturedRange ? receiptTotalCents : totalCents;
        cashup.setReceiptTotalCents(effectiveReceiptTotalCents);
        cashup.setVarianceCents(totalCents - effectiveReceiptTotalCents);
        cashup.setEmployeeResponsibleId(bookUsage.employee().id());
        cashup.setEmployeeResponsibleName(bookUsage.employee().name());
        cashup.setAreaCode(bookUsage.area().code());
        cashup.setAreaName(bookUsage.area().name());
        cashup.setNotes(clean(request.getNotes()));
        cashup.setCreatedBy(request.getUserId().trim());
        cashup.setUpdatedBy(request.getUserId().trim());
        cashup.setSyncedAt(LocalDateTime.now());
        cashup = cashupRepository.save(cashup);

        List<CashupReceiptEntity> cashupReceipts = new ArrayList<>();
        Map<String, Long> amountByMethod = new HashMap<>();
        Map<String, Integer> countByMethod = new HashMap<>();
        for (ManualPremiumReceiptEntity manualReceipt : selectedReceipts) {
            CashupReceiptEntity cashupReceipt = new CashupReceiptEntity();
            cashupReceipt.setCashup(cashup);
            cashupReceipt.setReceiptId(manualReceipt.getId());
            cashupReceipt.setReceiptNo(toLongReceiptNumber(manualReceipt.getManualReceiptNo()));
            cashupReceipt.setLegacyTransactionId(manualReceipt.getPaymentBatchId());
            cashupReceipt.setAmountCents(defaultLong(manualReceipt.getAmountCents()));
            cashupReceipt.setPaymentMethod(normalizePaymentMethod(manualReceipt.getPaymentMethod()));
            cashupReceipts.add(cashupReceipt);

            String method = normalizePaymentMethod(manualReceipt.getPaymentMethod());
            if (method == null) method = "UNKNOWN";
            amountByMethod.merge(method, defaultLong(manualReceipt.getAmountCents()), Long::sum);
            countByMethod.merge(method, 1, Integer::sum);
            manualReceipt.setCashupId(cashup.getId());
        }

        cashupReceiptRepository.saveAll(cashupReceipts);
        replacePaymentSummaries(cashup, amountByMethod, countByMethod);
        manualPremiumReceiptRepository.saveAll(selectedReceipts);
        return getCashup(cashup.getId());
    }

    @Transactional
    public CashupDepositResponse createDeposit(String cashupId, CashupDepositRequest request) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));

        if (isDepositExempt(cashup)) {
            throw new IllegalStateException("Deposits are not applicable to individual EFT cashups");
        }
        if (!canCaptureDeposit(cashup)) {
            throw new IllegalStateException("Deposits can only be created before the cashup is submitted for approval");
        }

        validateDepositRequest(request);

        CashupDepositEntity deposit = new CashupDepositEntity();
        deposit.setCashup(cashup);
        deposit.setDepositDate(parseDate(request.getDepositDate()));
        deposit.setAmountCents(defaultLong(request.getAmountCents()));
        deposit.setPaymentMethod(clean(request.getPaymentMethod()));
        deposit.setBankName(referenceDataValidationService.optionalOption(
                "BANK-NAME", request.getBankName(), "Bank name"));
        deposit.setReferenceNo(clean(request.getReferenceNo()));
        deposit.setNotes(clean(request.getNotes()));
        deposit.setCreatedBy(clean(request.getCreatedBy()));
        deposit.setUpdatedBy(clean(request.getCreatedBy()));

        deposit = cashupDepositRepository.save(deposit);

        AttachmentCreateDto attachmentRequest = new AttachmentCreateDto();
        attachmentRequest.setObjectType("CASHUP_DEPOSIT");
        attachmentRequest.setObjectId(deposit.getId());
        attachmentRequest.setDocumentType(request.getAttachmentDocumentType().trim());
        attachmentRequest.setExtension(request.getAttachmentExtension().trim());
        attachmentRequest.setFile(request.getAttachmentFile());
        try {
            AttachmentEntity proof = attachmentService.saveAndReturn(attachmentRequest);
            deposit.setProofAttachmentId(proof.getId());
            deposit = cashupDepositRepository.save(deposit);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store the required deposit attachment: " + ex.getMessage(), ex);
        }

        recalculateDeposits(cashup);
        return toDepositResponse(deposit);
    }

    @Transactional(readOnly = true)
    public List<CashupDepositResponse> getDeposits(String cashupId) {
        return cashupDepositRepository.findByCashupIdOrderByDepositDateDescCreatedAtDesc(cashupId)
                .stream()
                .map(this::toDepositResponse)
                .toList();
    }

    @Transactional
    public void deleteDeposit(String cashupId, String depositId) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));

        CashupDepositEntity deposit = cashupDepositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found: " + depositId));

        if (deposit.getCashup() == null || !cashupId.equals(deposit.getCashup().getId())) {
            throw new IllegalArgumentException("Deposit does not belong to cashup: " + cashupId);
        }

        if (STATUS_APPROVED.equalsIgnoreCase(cashup.getStatus()) || STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Deposits cannot be deleted after cashup submission");
        }

        if (clean(deposit.getProofAttachmentId()) != null) {
            try {
                attachmentService.delete(deposit.getProofAttachmentId());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to delete the deposit proof attachment", ex);
            }
        }
        cashupDepositRepository.delete(deposit);
        recalculateDeposits(cashup);
    }

    @Transactional
    public CashupResponse moveToAwaitingDeposits(String id, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (STATUS_AWAITING_DEPOSITS.equalsIgnoreCase(cashup.getStatus())) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is already awaiting deposits")
                    .build();
        }
        if (isDepositExempt(cashup)) {
            throw new IllegalStateException("This EFT cashup does not require a deposit stage");
        }
        if (!STATUS_OPEN.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only OPEN cashups can be moved to awaiting deposits");
        }

        recalculateDeposits(cashup);
        cashup.setStatus(STATUS_AWAITING_DEPOSITS);
        cashup.setUpdatedBy(clean(actionBy) == null ? cashup.getUserId() : actionBy.trim());
        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup moved to awaiting deposits")
                .build();
    }

    @Transactional
    public CashupResponse submitForApproval(String id, CashupSubmitForApprovalRequest request) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (cashup.getApprovalRequestId() != null && !cashup.getApprovalRequestId().isBlank()) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .approvalRequestId(cashup.getApprovalRequestId())
                    .message("Cashup already has an approval request")
                    .build();
        }

        if (!canSubmitForApproval(cashup)) {
            throw new IllegalStateException("Only cashups awaiting deposits can be submitted for approval");
        }

        recalculateDeposits(cashup);
        if (!isDepositExempt(cashup) && defaultInt(cashup.getDepositCount()) < 1) {
            throw new IllegalStateException("At least one deposit must be captured before the cashup can be submitted for approval");
        }
        String requesterId = request != null && request.getRequesterId() != null && !request.getRequesterId().isBlank()
                ? request.getRequesterId()
                : cashup.getUserId();

        ApprovalSubmitRequest approvalRequest = new ApprovalSubmitRequest();
        approvalRequest.setApprovalType(ApprovalType.CASHUP);
        approvalRequest.setReferenceId(cashup.getId());
        approvalRequest.setReferenceNo(String.valueOf(cashup.getCashupNo()));
        String cashierName = resolveCashierName(cashup.getUserId());
        approvalRequest.setTitle("Cashup " + cashup.getCashupNo() + " - " + cashierName
                + " - " + cashup.getCashupDate());
        approvalRequest.setDescription(isDepositExempt(cashup)
                ? "Cashup submitted for approval. Total collected: "
                    + defaultLong(cashup.getTotalCents()) + " cents. EFT payment - deposit not required."
                : "Cashup submitted for approval. Total collected: "
                    + defaultLong(cashup.getTotalCents())
                    + " cents, deposits: " + defaultLong(cashup.getDepositTotalCents()) + " cents.");
        approvalRequest.setRequesterId(requesterId);
        approvalRequest.setPayloadJson(toJson(toSummary(cashup)));

        ApprovalRequestResponse approvalResponse = approvalService.submitForApproval(approvalRequest);

        // ApprovalSubmissionHandler owns the submitted state. In AUTO mode the same
        // call may also complete the approval immediately, so do not overwrite the
        // handler's final APPROVED state here.
        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .approvalRequestId(approvalResponse.getId())
                .message("Cashup submitted for approval")
                .build();
    }

    @Transactional
    public CashupResponse approveCashup(String id, String approvedBy) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (!STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be approved");
        }

        cashup.setStatus(STATUS_APPROVED);
        cashup.setUpdatedBy(approvedBy);

        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup approved successfully")
                .build();
    }

    @Transactional
    public CashupResponse rejectCashup(String id, String rejectedBy, String reason) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (!STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be rejected");
        }

        cashup.setStatus(STATUS_REJECTED);
        cashup.setNotes(reason);
        cashup.setUpdatedBy(rejectedBy);

        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup rejected successfully")
                .build();
    }

    private void applyRequestToCashup(CashupEntity cashup, CashupRequest request, String requestedStatus, boolean created) {
        cashup.setCashupNo(request.getCashupNo());
        cashup.setDeviceId(request.getDeviceId());
        cashup.setUserId(request.getUserId());
        cashup.setCashupDate(parseDate(request.getDate()));
        cashup.setTotalCents(defaultLong(request.getTotalCents()));
        cashup.setReceiptCount(defaultInt(request.getReceiptCount()));
        cashup.setStatus(requestedStatus);
        cashup.setNotes(request.getNotes());
        cashup.setSyncedAt(LocalDateTime.now());

        if (created) {
            cashup.setCreatedBy(request.getUserId());
        }
        cashup.setUpdatedBy(request.getUserId());
    }

    private void replacePaymentSummaries(
            CashupEntity cashup,
            Map<String, Long> amountByMethod,
            Map<String, Integer> countByMethod
    ) {
        List<CashupPaymentSummaryEntity> existingSummaries =
                cashupPaymentSummaryRepository.findByCashupId(cashup.getId());

        Map<String, CashupPaymentSummaryEntity> existingByMethod = new HashMap<>();
        for (CashupPaymentSummaryEntity existing : existingSummaries) {
            String paymentMethod = normalizePaymentMethod(existing.getPaymentMethod());
            if (paymentMethod != null) {
                existingByMethod.put(paymentMethod, existing);
            }
        }

        if (amountByMethod == null || amountByMethod.isEmpty()) {
            if (!existingSummaries.isEmpty()) {
                cashupPaymentSummaryRepository.deleteAll(existingSummaries);
            }
            return;
        }

        List<CashupPaymentSummaryEntity> summariesToSave = new ArrayList<>();

        amountByMethod.forEach((method, amount) -> {
            String paymentMethod = normalizePaymentMethod(method);
            if (paymentMethod == null) {
                return;
            }

            CashupPaymentSummaryEntity entity = existingByMethod.remove(paymentMethod);
            if (entity == null) {
                entity = new CashupPaymentSummaryEntity();
                entity.setCashup(cashup);
                entity.setPaymentMethod(paymentMethod);
            }

            entity.setAmountCents(defaultLong(amount));
            entity.setPaymentCount(defaultInt(resolvePaymentCount(countByMethod, method, paymentMethod)));

            summariesToSave.add(entity);
        });

        if (!existingByMethod.isEmpty()) {
            cashupPaymentSummaryRepository.deleteAll(existingByMethod.values());
        }

        if (!summariesToSave.isEmpty()) {
            cashupPaymentSummaryRepository.saveAll(summariesToSave);
        }
    }

    private Integer resolvePaymentCount(Map<String, Integer> countByMethod, String originalMethod, String normalizedMethod) {
        if (countByMethod == null || countByMethod.isEmpty()) {
            return null;
        }

        Integer count = countByMethod.get(originalMethod);
        if (count != null) {
            return count;
        }

        count = countByMethod.get(normalizedMethod);
        if (count != null) {
            return count;
        }

        for (Map.Entry<String, Integer> entry : countByMethod.entrySet()) {
            if (normalizedMethod.equals(normalizePaymentMethod(entry.getKey()))) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String normalizePaymentMethod(String method) {
        String value = clean(method);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private void replaceReceipts(CashupEntity cashup, List<CashupReceiptRequest> receipts) {
        List<CashupReceiptEntity> existing = cashupReceiptRepository.findByCashupId(cashup.getId());

        if (receipts == null || receipts.isEmpty()) {
            if (!existing.isEmpty()) {
                cashupReceiptRepository.deleteAll(existing);
            }
            return;
        }

        Map<String, CashupReceiptEntity> existingByKey = new HashMap<>();
        for (CashupReceiptEntity entity : existing) {
            String key = receiptKey(entity.getReceiptId(), entity.getReceiptNo());
            if (key != null) {
                existingByKey.put(key, entity);
            }
        }

        Map<String, CashupReceiptRequest> incomingByKey = new LinkedHashMap<>();
        int rowIndex = 0;
        for (CashupReceiptRequest item : receipts) {
            if (item == null) continue;
            String key = receiptKey(item.getReceiptId(), item.getReceiptNo());
            if (key == null) {
                key = "ROW:" + rowIndex;
            }
            rowIndex++;
            incomingByKey.put(key, item);
        }

        List<CashupReceiptEntity> entitiesToSave = new ArrayList<>();
        for (Map.Entry<String, CashupReceiptRequest> entry : incomingByKey.entrySet()) {
            CashupReceiptRequest item = entry.getValue();
            CashupReceiptEntity entity = existingByKey.remove(entry.getKey());
            if (entity == null) {
                entity = new CashupReceiptEntity();
                entity.setCashup(cashup);
            }
            entity.setReceiptId(clean(item.getReceiptId()));
            entity.setReceiptNo(item.getReceiptNo());
            entity.setAmountCents(defaultLong(item.getAmountCents()));
            entity.setPaymentMethod(normalizePaymentMethod(item.getPaymentMethod()));
            entitiesToSave.add(entity);
        }

        if (!existingByKey.isEmpty()) {
            cashupReceiptRepository.deleteAll(existingByKey.values());
        }
        if (!entitiesToSave.isEmpty()) {
            cashupReceiptRepository.saveAll(entitiesToSave);
        }
    }

    private String receiptKey(String receiptId, Long receiptNo) {
        String id = clean(receiptId);
        if (id != null) return "ID:" + id;
        return receiptNo == null ? null : "NO:" + receiptNo;
    }

    private CashupSummaryResponse toSummary(CashupEntity cashup) {
        return CashupSummaryResponse.builder()
                .id(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .deviceId(cashup.getDeviceId())
                .userId(cashup.getUserId())
                .cashierName(resolveCashierName(cashup.getUserId()))
                .cashupDate(cashup.getCashupDate())
                .totalCents(cashup.getTotalCents())
                .receiptCount(cashup.getReceiptCount())
                .status(cashup.getStatus())
                .source(cashup.getSource())
                .receiptBookNo(cashup.getReceiptBookNo())
                .receiptFromNo(cashup.getReceiptFromNo())
                .receiptToNo(cashup.getReceiptToNo())
                .manualAmountCents(cashup.getManualAmountCents())
                .receiptTotalCents(cashup.getReceiptTotalCents())
                .varianceCents(cashup.getVarianceCents())
                .employeeResponsibleId(cashup.getEmployeeResponsibleId())
                .employeeResponsibleName(cashup.getEmployeeResponsibleName())
                .areaCode(cashup.getAreaCode())
                .areaName(cashup.getAreaName())
                .depositTotalCents(defaultLong(cashup.getDepositTotalCents()))
                .depositCount(defaultInt(cashup.getDepositCount()))
                .approvalRequestId(cashup.getApprovalRequestId())
                .deposits(getDeposits(cashup.getId()))
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialise cashup approval details", exception);
        }
    }

    @Transactional
    public void markSubmittedFromApproval(String cashupId, String approvalRequestId, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));
        cashup.setStatus(STATUS_SUBMITTED);
        cashup.setApprovalRequestId(approvalRequestId);
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    @Transactional
    public void markApprovedFromApproval(String cashupId, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));
        cashup.setStatus(STATUS_APPROVED);
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    private void validateDepositRequest(CashupDepositRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Deposit request is required");
        }
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (request.getDepositDate() == null || request.getDepositDate().isBlank()) {
            throw new IllegalArgumentException("depositDate is required");
        }
        if (clean(request.getAttachmentFile()) == null) {
            throw new IllegalArgumentException("A proof-of-deposit attachment is required");
        }
        if (clean(request.getAttachmentExtension()) == null) {
            throw new IllegalArgumentException("attachmentExtension is required");
        }
        if (clean(request.getAttachmentDocumentType()) == null) {
            throw new IllegalArgumentException("attachmentDocumentType is required");
        }
    }

    private void recalculateDeposits(CashupEntity cashup) {
        List<CashupDepositEntity> deposits = cashupDepositRepository.findByCashupIdOrderByDepositDateDescCreatedAtDesc(cashup.getId());
        long total = deposits.stream().mapToLong(item -> defaultLong(item.getAmountCents())).sum();
        cashup.setDepositTotalCents(total);
        cashup.setDepositCount(deposits.size());
        cashupRepository.save(cashup);
    }

    private CashupDepositResponse toDepositResponse(CashupDepositEntity entity) {
        return CashupDepositResponse.builder()
                .id(entity.getId())
                .cashupId(entity.getCashup() != null ? entity.getCashup().getId() : null)
                .depositDate(entity.getDepositDate())
                .amountCents(defaultLong(entity.getAmountCents()))
                .paymentMethod(entity.getPaymentMethod())
                .bankName(entity.getBankName())
                .referenceNo(entity.getReferenceNo())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .proofAttachmentId(entity.getProofAttachmentId())
                .build();
    }

    private void validateManualCashupRequest(ManualCashupCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("Manual cashup request is required");
        if (clean(request.getReceiptFromNo()) == null) throw new IllegalArgumentException("receiptFromNo is required");
        if (clean(request.getReceiptToNo()) == null) throw new IllegalArgumentException("receiptToNo is required");
        if (clean(request.getUserId()) == null) throw new IllegalArgumentException("userId is required");
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) throw new IllegalArgumentException("amountCents must be greater than zero");
        if (clean(request.getEmployeeResponsibleId()) == null) throw new IllegalArgumentException("employeeResponsibleId is required");
        if (clean(request.getAreaCode()) == null) throw new IllegalArgumentException("areaCode is required");
    }

    private void preventOverlappingManualCashup(
            String receiptBookNo,
            BigInteger requestedFrom,
            BigInteger requestedTo) {
        List<CashupEntity> existingCashups = cashupRepository
                .findBySourceAndReceiptBookNoIgnoreCaseOrderByCreatedAtAsc(
                        SOURCE_MANUAL_RECEIPT_BOOK, receiptBookNo);
        for (CashupEntity existing : existingCashups) {
            String existingFromValue = clean(existing.getReceiptFromNo());
            String existingToValue = clean(existing.getReceiptToNo());
            if (existingFromValue == null || existingToValue == null) continue;
            BigInteger existingFrom = parseManualReceiptNumber(existingFromValue, "receiptFromNo");
            BigInteger existingTo = parseManualReceiptNumber(existingToValue, "receiptToNo");
            boolean overlaps = requestedFrom.compareTo(existingTo) <= 0
                    && requestedTo.compareTo(existingFrom) >= 0;
            if (overlaps) {
                throw new IllegalStateException(
                        "Receipt range overlaps manual cashup #" + existing.getCashupNo()
                                + " (" + existingFrom + " - " + existingTo + ")");
            }
        }
    }

    private int manualReceiptRangeCount(BigInteger fromNo, BigInteger toNo) {
        BigInteger count = toNo.subtract(fromNo).add(BigInteger.ONE);
        if (count.signum() <= 0) {
            throw new IllegalArgumentException("Receipt range must contain at least one receipt");
        }
        if (count.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Receipt range is too large");
        }
        return count.intValue();
    }

    private BigInteger parseManualReceiptNumber(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null || !cleaned.matches("\\d+")) {
            throw new IllegalArgumentException(fieldName + " must contain digits only");
        }
        return new BigInteger(cleaned);
    }

    private Long toLongReceiptNumber(String value) {
        try {
            return parseManualReceiptNumber(value, "manualReceiptNo").longValueExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private Map<String, String> resolveCashierNames(List<CashupEntity> cashups) {
        Set<String> userIds = cashups.stream()
                .map(CashupEntity::getUserId)
                .filter(id -> clean(id) != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of();

        Map<String, UserEntity> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Set<String> partnerIds = users.values().stream()
                .map(UserEntity::getPartner)
                .filter(id -> clean(id) != null)
                .collect(Collectors.toSet());
        Map<String, PartnerEntity> partners = partnerRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(PartnerEntity::getId, Function.identity()));

        Map<String, String> names = new HashMap<>();
        for (String userId : userIds) {
            names.put(userId, displayName(users.get(userId), partners));
        }
        return names;
    }

    private String resolveCashierName(String userId) {
        String cleanedUserId = clean(userId);
        if (cleanedUserId == null) return "Unknown cashier";
        UserEntity user = userRepository.findById(cleanedUserId).orElse(null);
        if (user == null) return "Unknown cashier";
        Map<String, PartnerEntity> partners = new HashMap<>();
        if (clean(user.getPartner()) != null) {
            partnerRepository.findById(user.getPartner()).ifPresent(partner -> partners.put(partner.getId(), partner));
        }
        return displayName(user, partners);
    }

    private String displayName(UserEntity user, Map<String, PartnerEntity> partners) {
        if (user == null) return "Unknown cashier";
        PartnerEntity partner = clean(user.getPartner()) == null ? null : partners.get(user.getPartner());
        if (partner != null) {
            List<String> parts = new ArrayList<>();
            if (clean(partner.getName2()) != null) parts.add(partner.getName2().trim());
            if (clean(partner.getName3()) != null) parts.add(partner.getName3().trim());
            if (clean(partner.getName1()) != null) parts.add(partner.getName1().trim());
            if (!parts.isEmpty()) return String.join(" ", parts);
        }
        if (clean(user.getUsername()) != null) return user.getUsername().trim();
        return "Unknown cashier";
    }

    private String partnerDisplayName(PartnerEntity partner) {
        if (partner == null) return null;
        String name = java.util.stream.Stream.of(partner.getName1(), partner.getName2(), partner.getName3())
                .map(this::clean)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" "));
        return name.isBlank() ? (clean(partner.getNo()) == null ? partner.getId() : partner.getNo()) : name;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateRequest(CashupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cashup request is required");
        }

        if (request.getCashupNo() == null) {
            throw new IllegalArgumentException("cashupNo is required");
        }

        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new IllegalArgumentException("date is required");
        }

        if (request.getTotalCents() == null) {
            throw new IllegalArgumentException("totalCents is required");
        }

        if (request.getReceiptCount() == null) {
            throw new IllegalArgumentException("receiptCount is required");
        }
    }

    private String resolveStatus(CashupRequest request, boolean mawaPayEft) {
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String status = request.getStatus().trim().toUpperCase();
            if (STATUS_OPEN.equals(status)) {
                return STATUS_OPEN;
            }
            if (STATUS_AWAITING_DEPOSITS.equals(status) || "DEPOSIT_PENDING".equals(status)) {
                return STATUS_AWAITING_DEPOSITS;
            }
            if (STATUS_COMPLETED.equals(status) || "CLOSED".equals(status)) {
                // Backwards compatibility: earlier MAWAPay builds used COMPLETED to mean
                // "cashier closed the device cashup". In ERP this must be the deposit stage.
                return STATUS_AWAITING_DEPOSITS;
            }
            if (STATUS_SUBMITTED.equals(status)) {
                if (mawaPayEft) {
                    // The MawaPay EFT flow creates one cashup per payment and
                    // explicitly marks it as deposit-exempt. Keep it OPEN just long
                    // enough for submitCashup() to create the normal CASHUP approval.
                    return STATUS_OPEN;
                }
                // Backwards compatibility: older MAWAPay clients used SUBMITTED to mean
                // "cashier closed the device cashup". In ERP this must still be editable
                // for deposits before it is submitted for approval.
                return STATUS_AWAITING_DEPOSITS;
            }
            throw new IllegalArgumentException("Invalid cashup status from device: " + request.getStatus());
        }

        String notes = request.getNotes() == null ? "" : request.getNotes().toUpperCase();
        if (notes.contains("LOCAL STATUS: OPEN") || notes.contains("LOCAL STATUS: ACTIVE_CASHUP")) {
            return STATUS_OPEN;
        }
        if (notes.contains("LOCAL STATUS: AWAITING_DEPOSITS")
                || notes.contains("LOCAL STATUS: DEPOSIT_PENDING")
                || notes.contains("LOCAL STATUS: COMPLETED")
                || notes.contains("LOCAL STATUS: SUBMITTED")
                || notes.contains("LOCAL STATUS: CLOSED")) {
            return STATUS_AWAITING_DEPOSITS;
        }

        // Backwards compatibility for older clients that used this endpoint only at final cashup time.
        return STATUS_AWAITING_DEPOSITS;
    }

    private boolean isLocked(CashupEntity cashup) {
        return STATUS_APPROVED.equalsIgnoreCase(cashup.getStatus())
                || STATUS_REJECTED.equalsIgnoreCase(cashup.getStatus());
    }

    private boolean isClosedForDeviceSync(CashupEntity cashup) {
        return STATUS_AWAITING_DEPOSITS.equalsIgnoreCase(cashup.getStatus())
                || STATUS_COMPLETED.equalsIgnoreCase(cashup.getStatus())
                || STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus());
    }

    private boolean canCaptureDeposit(CashupEntity cashup) {
        if (isDepositExempt(cashup)) return false;
        String status = cashup.getStatus() == null ? "" : cashup.getStatus().trim().toUpperCase(Locale.ROOT);
        return STATUS_AWAITING_DEPOSITS.equals(status) || STATUS_COMPLETED.equals(status);
    }

    private boolean canSubmitForApproval(CashupEntity cashup) {
        String status = cashup.getStatus() == null ? "" : cashup.getStatus().trim().toUpperCase(Locale.ROOT);
        return isDepositExempt(cashup)
                ? isPreApprovalStatus(status)
                : STATUS_AWAITING_DEPOSITS.equals(status) || STATUS_COMPLETED.equals(status);
    }

    private boolean isPreApprovalStatus(String status) {
        return STATUS_AWAITING_DEPOSITS.equals(status)
                || STATUS_COMPLETED.equals(status) // Legacy compatibility for already-synced cashups
                || STATUS_OPEN.equals(status)
                || "DRAFT".equals(status)
                || "NEW".equals(status);
    }

    private boolean isDepositExempt(CashupEntity cashup) {
        if (cashup == null) return false;
        String source = clean(cashup.getSource());
        return SOURCE_ERP_ONLINE_EFT.equalsIgnoreCase(source)
                || SOURCE_MAWA_PAY_EFT.equalsIgnoreCase(source);
    }

    private boolean isMawaPayIndividualEftCashup(CashupRequest request) {
        if (request == null || request.getReceiptCount() == null || request.getReceiptCount() <= 0) {
            return false;
        }

        String notes = request.getNotes() == null
                ? ""
                : request.getNotes().trim().toUpperCase(Locale.ROOT);
        if (!notes.contains("SOURCE: MAWA_PAY_EFT")) {
            return false;
        }

        String method = eftPaymentMethod(request);
        if (!"EFT".equals(method)) {
            return false;
        }

        Integer paymentCount = resolvePaymentCount(
                request.getCountByMethod(),
                method,
                method
        );
        return paymentCount != null && paymentCount == 1;
    }

    private String eftPaymentMethod(CashupRequest request) {
        if (request == null || request.getAmountByMethod() == null
                || request.getAmountByMethod().size() != 1) {
            return null;
        }
        return normalizePaymentMethod(
                request.getAmountByMethod().keySet().iterator().next()
        );
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Continue below
        }

        try {
            return Instant.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Continue below
        }

        try {
            return LocalDateTime.parse(value)
                    .toLocalDate();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid cashup date: " + value);
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
