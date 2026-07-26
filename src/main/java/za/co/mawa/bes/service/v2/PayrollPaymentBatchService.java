package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchAuditEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;
import za.co.mawa.bes.enums.payroll.PayrollPaymentItemStatus;
import za.co.mawa.bes.fnb.FnbInitiationRecoveryService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.BankPaymentResponse;
import za.co.mawa.bes.fnb.dto.OriginalPaymentInformation;
import za.co.mawa.bes.fnb.dto.StatusReasonInformation;
import za.co.mawa.bes.fnb.dto.TransactionInfoAndStatus;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentBatchAuditRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentBatchRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentItemRepository;
import za.co.mawa.bes.service.MessageProducerService;
import za.co.mawa.bes.service.PartnerBankAccountService;
import za.co.mawa.bes.utils.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PayrollPaymentBatchService {
    public static final String PAYMENT_MESSAGE_TYPE = "FNB-PAYROLL-PAYMENT";
    public static final String REPORT_MESSAGE_TYPE = "FNB-PAYROLL-PAYMENT-REPORT";

    private final PayrollPaymentBatchRepository batchRepository;
    private final PayrollPaymentItemRepository itemRepository;
    private final PayrollPaymentBatchAuditRepository auditRepository;
    private final ReferenceDataValidationService referenceDataValidationService;
    private final PartnerBankAccountService partnerBankAccountService;
    private final EmploymentRepository employmentRepository;
    private final PaymentAccountConfigurationService paymentAccountConfigurationService;
    private final PayrollBankMessageFactory bankMessageFactory;
    private final MessageProducerService messageProducerService;
    private final za.co.mawa.bes.fnb.v2.BankPaymentService bankPaymentService;
    private final FnbInitiationRecoveryService fnbInitiationRecoveryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PayrollPaymentBatchResponse createBatch(PayrollPaymentBatchCreateRequest request, String userId) {
        validateCreateRequest(request);
        batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
            throw new IllegalArgumentException("Payroll payment batch number already exists: " + request.getBatchNo());
        });
        PayrollPaymentBatchEntity batch = new PayrollPaymentBatchEntity();
        batch.setBatchNo(request.getBatchNo().trim());
        batch.setDescription(request.getDescription());
        batch.setPayPeriod(request.getPayPeriod());
        batch.setPaymentDate(request.getPaymentDate());
        batch.setNotes(request.getNotes());
        batch.setStatus(PayrollPaymentBatchStatus.DRAFT);
        batch.setCreatedBy(user(userId));
        batch = batchRepository.save(batch);
        if (request.getItems() != null) {
            for (PayrollPaymentItemRequest itemRequest : request.getItems()) {
                PayrollPaymentItemEntity item = toItemEntity(itemRequest);
                item.setBatchId(batch.getId());
                item.setCreatedBy(user(userId));
                itemRepository.save(item);
            }
        }
        recalculateBatchTotals(batch.getId());
        createAudit(batch.getId(), "CREATED", null, "DRAFT", "Payroll payment batch created", user(userId));
        return getBatch(batch.getId());
    }

    @Transactional
    public PayrollPaymentBatchResponse editBatch(String batchId, PayrollPaymentBatchEditRequest request, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT payroll payment batches can be edited");
        }
        if (hasText(request.getBatchNo())) {
            batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
                if (!existing.getId().equals(batchId)) {
                    throw new IllegalArgumentException("Payroll payment batch number already exists: " + request.getBatchNo());
                }
            });
            batch.setBatchNo(request.getBatchNo().trim());
        }
        if (request.getDescription() != null) batch.setDescription(request.getDescription());
        if (hasText(request.getPayPeriod())) {
            validatePayPeriod(request.getPayPeriod());
            batch.setPayPeriod(request.getPayPeriod());
        }
        if (request.getPaymentDate() != null) batch.setPaymentDate(request.getPaymentDate());
        if (request.getNotes() != null) batch.setNotes(request.getNotes());
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);

        if (request.getItems() != null) {
            for (PayrollPaymentItemEditRequest itemRequest : request.getItems()) {
                PayrollPaymentItemEntity item = itemRepository.findById(itemRequest.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Payroll payment item not found: " + itemRequest.getId()));
                if (!batchId.equals(item.getBatchId())) throw new IllegalArgumentException("Item does not belong to batch");
                if (itemRequest.getAmountCents() != null) {
                    if (itemRequest.getAmountCents() <= 0) throw new IllegalArgumentException("Item amount must be greater than zero");
                    item.setAmountCents(itemRequest.getAmountCents());
                }
                refreshApprovedBankDetails(item);
                item.setUpdatedBy(user(userId));
                itemRepository.save(item);
            }
        }
        recalculateBatchTotals(batchId);
        createAudit(batchId, "EDITED", "DRAFT", "DRAFT", "Payroll payment batch edited", user(userId));
        return getBatch(batchId);
    }

    @Transactional
    public PayrollPaymentBatchResponse copyPreviousBatch(String sourceBatchId, PayrollPaymentBatchCopyRequest request, String userId) {
        PayrollPaymentBatchEntity source = requireBatch(sourceBatchId);
        validateCopyRequest(request);
        batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
            throw new IllegalArgumentException("Payroll payment batch number already exists: " + request.getBatchNo());
        });
        PayrollPaymentBatchEntity batch = new PayrollPaymentBatchEntity();
        batch.setBatchNo(request.getBatchNo());
        batch.setDescription(request.getDescription());
        batch.setPayPeriod(request.getPayPeriod());
        batch.setPaymentDate(request.getPaymentDate());
        batch.setSourceBatchId(source.getId());
        batch.setStatus(PayrollPaymentBatchStatus.DRAFT);
        batch.setNotes(request.getNotes());
        batch.setCreatedBy(user(userId));
        batch = batchRepository.save(batch);
        List<PayrollPaymentItemEntity> sourceItems = Boolean.TRUE.equals(request.getCopyExcludedItems())
                ? itemRepository.findByBatchIdOrderByEmployeeNameAsc(sourceBatchId)
                : itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(sourceBatchId);
        for (PayrollPaymentItemEntity sourceItem : sourceItems) {
            PayrollPaymentItemEntity item = new PayrollPaymentItemEntity();
            item.setBatchId(batch.getId());
            item.setEmployeeId(sourceItem.getEmployeeId());
            item.setEmployeeNo(sourceItem.getEmployeeNo());
            item.setEmployeeName(sourceItem.getEmployeeName());
            item.setAmountCents(sourceItem.getAmountCents());
            item.setPaymentReference(sourceItem.getPaymentReference());
            item.setSalaryReference(sourceItem.getSalaryReference());
            item.setStatus(PayrollPaymentItemStatus.PENDING);
            item.setExcluded(false);
            item.setCreatedBy(user(userId));
            refreshApprovedBankDetails(item);
            itemRepository.save(item);
        }
        recalculateBatchTotals(batch.getId());
        createAudit(batch.getId(), "COPIED", null, "DRAFT", "Copied from " + source.getBatchNo(), user(userId));
        return getBatch(batch.getId());
    }

    public PayrollPaymentBatchResponse getBatch(String batchId) {
        return toBatchResponse(requireBatch(batchId), itemRepository.findByBatchIdOrderByEmployeeNameAsc(batchId));
    }

    public List<PayrollPaymentBatchResponse> getByPayPeriod(String payPeriod) {
        return batchRepository.findByPayPeriodOrderByCreatedAtDesc(payPeriod).stream()
                .map(batch -> toBatchResponse(batch, itemRepository.findByBatchIdOrderByEmployeeNameAsc(batch.getId())))
                .toList();
    }

    @Transactional
    public void prepareVerificationPrintout(String batchId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (!hasText(batch.getFnbInstructionId())
                && batch.getStatus() != PayrollPaymentBatchStatus.CANCELLED
                && batch.getStatus() != PayrollPaymentBatchStatus.REJECTED
                && batch.getStatus() != PayrollPaymentBatchStatus.FAILED
                && batch.getStatus() != PayrollPaymentBatchStatus.PAID) {
            validateAndRefreshItems(batchId);
        }
    }

    @Transactional
    public PayrollPaymentBatchResponse addItem(String batchId, PayrollPaymentItemRequest request, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new IllegalStateException("Items can only be added to a DRAFT payroll payment batch");
        }
        PayrollPaymentItemEntity item = toItemEntity(request);
        item.setBatchId(batchId);
        item.setCreatedBy(user(userId));
        itemRepository.save(item);
        recalculateBatchTotals(batchId);
        createAudit(batchId, "ITEM_ADDED", "DRAFT", "DRAFT", "Employee payment added", user(userId));
        return getBatch(batchId);
    }

    @Transactional
    public void markPendingApproval(String batchId, String approvalRequestId, String actionBy) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new IllegalStateException("Only a draft payroll batch can be submitted for approval");
        }
        validateAndRefreshItems(batchId);
        PayrollPaymentBatchStatus old = batch.getStatus();
        batch.setStatus(PayrollPaymentBatchStatus.PENDING_APPROVAL);
        batch.setApprovalRequestId(approvalRequestId);
        batch.setUpdatedBy(user(actionBy));
        batchRepository.save(batch);
        createAudit(batchId, "SUBMITTED_FOR_APPROVAL", old.name(), batch.getStatus().name(),
                "Payroll batch submitted for approval", user(actionBy));
    }

    @Transactional
    public PayrollPaymentBatchResponse approveBatch(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() != PayrollPaymentBatchStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Payroll batch is not awaiting approval");
        }
        validateAndRefreshItems(batchId);
        PayrollPaymentBatchStatus old = batch.getStatus();
        batch.setStatus(PayrollPaymentBatchStatus.APPROVED);
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);
        createAudit(batchId, "APPROVED", old.name(), "APPROVED", "Payroll payment batch approved", user(userId));
        queueBankMessage(batchId, userId);
        return getBatch(batchId);
    }

    @Transactional
    public void rejectFromApproval(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        PayrollPaymentBatchStatus old = batch.getStatus();
        batch.setStatus(PayrollPaymentBatchStatus.REJECTED);
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);
        createAudit(batchId, "REJECTED", old.name(), "REJECTED", "Payroll approval rejected", user(userId));
    }

    @Transactional
    public PayrollPaymentBatchResponse cancelBatch(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() == PayrollPaymentBatchStatus.PROCESSING
                || batch.getStatus() == PayrollPaymentBatchStatus.SUBMITTED
                || batch.getStatus() == PayrollPaymentBatchStatus.PAID) {
            throw new IllegalStateException("Payroll batch cannot be cancelled after bank processing has started");
        }
        PayrollPaymentBatchStatus old = batch.getStatus();
        batch.setStatus(PayrollPaymentBatchStatus.CANCELLED);
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);
        createAudit(batchId, "CANCELLED", old.name(), "CANCELLED", "Payroll payment batch cancelled", user(userId));
        return getBatch(batchId);
    }

    @Transactional
    public PayrollPaymentBatchResponse queueBankMessage(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (batch.getStatus() != PayrollPaymentBatchStatus.APPROVED
                && batch.getStatus() != PayrollPaymentBatchStatus.PROCESSING) {
            throw new IllegalStateException("Only an approved payroll batch can be sent to the bank");
        }
        if (hasText(batch.getFnbInstructionId())) return getBatch(batchId);
        List<PayrollPaymentItemEntity> items = validateAndRefreshItems(batchId);
        Map<String, Object> debtor = paymentAccountConfigurationService.activePayrollDebtor()
                .orElseThrow(() -> new IllegalStateException(
                        "Maintain an active PAYROLL_DEBTOR account in Payment Account Configuration"));
        if (!"FNB".equalsIgnoreCase(String.valueOf(debtor.get("bank_integration")))) {
            throw new IllegalStateException("Automated payroll submission currently requires an FNB payroll debtor account");
        }
        BankPaymentRequest request = bankMessageFactory.build(batch, items, debtor);
        try {
            MessageQueueInboundDto message = new MessageQueueInboundDto();
            message.setType(PAYMENT_MESSAGE_TYPE);
            message.setReferenceId(batchId);
            message.setReferenceNo(batch.getBatchNo());
            message.setPayload(objectMapper.writeValueAsString(request));
            messageProducerService.sendMessageIfNotExists(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to queue payroll bank message: " + exception.getMessage(), exception);
        }
        batch.setDebtorAccountId(String.valueOf(debtor.get("id")));
        batch.setBankMessageStatus("QUEUED");
        batch.setBankQueuedAt(LocalDateTime.now());
        batch.setStatus(PayrollPaymentBatchStatus.PROCESSING);
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);
        createAudit(batchId, "BANK_QUEUED", "APPROVED", "PROCESSING",
                "One FNB message queued with " + items.size() + " employee creditors", user(userId));
        return getBatch(batchId);
    }

    @Transactional
    public String processBankSubmission(String batchId, String payload, String userId) throws Exception {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (hasText(batch.getFnbInstructionId())) {
            queueBankReport(batch);
            return batch.getFnbInstructionId();
        }
        String instructionId = fnbInitiationRecoveryService.recoverInstructionId(List.of(batch.getBatchNo()));
        if (!hasText(instructionId)) {
            instructionId = bankPaymentService.sendPaymentRequest(payload);
        }
        if (!hasText(instructionId)) throw new IllegalStateException("FNB returned no instruction ID for payroll batch");
        batch.setFnbInstructionId(instructionId);
        batch.setBankMessageStatus("SUBMITTED");
        batch.setBankSubmittedAt(LocalDateTime.now());
        batch.setStatus(PayrollPaymentBatchStatus.SUBMITTED);
        batch.setUpdatedBy(user(userId));
        batchRepository.save(batch);
        List<PayrollPaymentItemEntity> items = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);
        for (PayrollPaymentItemEntity item : items) {
            item.setStatus(PayrollPaymentItemStatus.SUBMITTED);
            itemRepository.save(item);
        }
        queueBankReport(batch);
        createAudit(batchId, "BANK_SUBMITTED", "PROCESSING", "SUBMITTED",
                "FNB instruction ID " + instructionId + " saved", user(userId));
        return instructionId;
    }

    @Transactional
    public boolean processBankReport(String batchId, String userId) throws Exception {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        if (!hasText(batch.getFnbInstructionId())) {
            throw new IllegalStateException("Payroll batch does not have an FNB instruction ID");
        }
        BankPaymentResponse report = bankPaymentService.getPaymentReport(batch.getFnbInstructionId());
        String providerStatus = resolveProviderStatus(report);
        String reason = resolveProviderReason(report);
        batch.setBankReportStatus(providerStatus);
        batch.setBankReportReason(reason);
        batch.setBankReportJson(objectMapper.writeValueAsString(report));
        batch.setBankReportRetrievedAt(LocalDateTime.now());
        batch.setUpdatedBy(user(userId));
        boolean finalStatus = false;
        PayrollPaymentItemStatus itemStatus = PayrollPaymentItemStatus.SUBMITTED;
        if (isSuccessfulBankStatus(providerStatus)) {
            batch.setStatus(PayrollPaymentBatchStatus.PAID);
            batch.setBankMessageStatus("PAID");
            itemStatus = PayrollPaymentItemStatus.PAID;
            finalStatus = true;
        } else if (isFailedBankStatus(providerStatus)) {
            batch.setStatus(PayrollPaymentBatchStatus.FAILED);
            batch.setBankMessageStatus("FAILED");
            itemStatus = PayrollPaymentItemStatus.FAILED;
            finalStatus = true;
        }
        batchRepository.save(batch);
        for (PayrollPaymentItemEntity item : itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId)) {
            item.setStatus(itemStatus);
            item.setFailureReason(itemStatus == PayrollPaymentItemStatus.FAILED ? reason : null);
            itemRepository.save(item);
        }
        createAudit(batchId, "BANK_REPORT", null, batch.getStatus().name(),
                "FNB report status: " + providerStatus, user(userId));
        return finalStatus;
    }

    public PayrollPaymentBatchResponse refreshBankReport(String batchId, String userId) {
        try {
            processBankReport(batchId, userId);
            return getBatch(batchId);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to retrieve payroll bank report: " + exception.getMessage(), exception);
        }
    }

    @Transactional
    public void recalculateBatchTotals(String batchId) {
        PayrollPaymentBatchEntity batch = requireBatch(batchId);
        List<PayrollPaymentItemEntity> payable = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);
        batch.setTotalEmployees(payable.size());
        batch.setTotalAmountCents(payable.stream().mapToLong(item -> item.getAmountCents() == null ? 0L : item.getAmountCents()).sum());
        batchRepository.save(batch);
    }

    private void queueBankReport(PayrollPaymentBatchEntity batch) {
        MessageQueueInboundDto report = new MessageQueueInboundDto();
        report.setType(REPORT_MESSAGE_TYPE);
        report.setReferenceId(batch.getId());
        report.setReferenceNo(batch.getFnbInstructionId());
        report.setPayload(batch.getFnbInstructionId());
        messageProducerService.sendMessageIfNotExists(report);
    }

    private List<PayrollPaymentItemEntity> validateAndRefreshItems(String batchId) {
        List<PayrollPaymentItemEntity> items = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);
        if (items.isEmpty()) throw new IllegalStateException("Payroll batch has no payable employee items");
        for (PayrollPaymentItemEntity item : items) {
            refreshApprovedBankDetails(item);
            if (item.getAmountCents() == null || item.getAmountCents() <= 0) {
                throw new IllegalStateException("Payroll amount must be greater than zero for " + item.getEmployeeName());
            }
            item.setStatus(PayrollPaymentItemStatus.VALIDATED);
            itemRepository.save(item);
        }
        recalculateBatchTotals(batchId);
        return items;
    }

    private PayrollPaymentItemEntity toItemEntity(PayrollPaymentItemRequest request) {
        if (!hasText(request.getEmployeeId())) throw new IllegalArgumentException("Employee is required");
        if (!hasText(request.getEmployeeName())) throw new IllegalArgumentException("Employee name is required");
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        PayrollPaymentItemEntity item = new PayrollPaymentItemEntity();
        item.setEmployeeId(request.getEmployeeId());
        item.setEmployeeNo(request.getEmployeeNo());
        item.setEmployeeName(request.getEmployeeName());
        item.setAmountCents(request.getAmountCents());
        item.setPaymentReference(request.getPaymentReference());
        item.setSalaryReference(request.getSalaryReference());
        item.setStatus(PayrollPaymentItemStatus.PENDING);
        item.setExcluded(false);
        refreshApprovedBankDetails(item);
        return item;
    }

    private void refreshApprovedBankDetails(PayrollPaymentItemEntity item) {
        if (!hasText(item.getEmployeeId())) throw new IllegalArgumentException("Employee partner ID is required");
        if (!employmentRepository.existsByPartnerIdAndStatus(item.getEmployeeId(), Status.ACTIVE)) {
            throw new IllegalStateException("Selected partner is not an active employee: " + item.getEmployeeName());
        }
        PartnerBankAccountEntity account = partnerBankAccountService.getActiveBankAccountEntity(item.getEmployeeId());
        item.setBankName(referenceDataValidationService.requireOption("BANK-NAME", account.getBankName(), "Bank name"));
        item.setBranchCode(account.getBranchCode());
        item.setAccountNo(account.getAccountNumber());
        item.setAccountType(referenceDataValidationService.requireOption(
                "BANK-ACCOUNT-TYPE", account.getAccountType(), "Bank account type"));
        item.setAccountHolderName(account.getAccountHolder());
    }

    private PayrollPaymentBatchResponse toBatchResponse(PayrollPaymentBatchEntity batch, List<PayrollPaymentItemEntity> items) {
        PayrollPaymentBatchResponse response = new PayrollPaymentBatchResponse();
        response.setId(batch.getId());
        response.setBatchNo(batch.getBatchNo());
        response.setDescription(batch.getDescription());
        response.setPayPeriod(batch.getPayPeriod());
        response.setPaymentDate(batch.getPaymentDate());
        response.setSourceBatchId(batch.getSourceBatchId());
        response.setStatus(batch.getStatus());
        response.setTotalEmployees(batch.getTotalEmployees());
        response.setTotalAmountCents(batch.getTotalAmountCents());
        response.setEftFileGenerated(batch.getEftFileGenerated());
        response.setEftFileName(batch.getEftFileName());
        response.setEftFileGeneratedAt(batch.getEftFileGeneratedAt());
        response.setNotes(batch.getNotes());
        response.setApprovalRequestId(batch.getApprovalRequestId());
        response.setDebtorAccountId(batch.getDebtorAccountId());
        response.setBankMessageStatus(batch.getBankMessageStatus());
        response.setFnbInstructionId(batch.getFnbInstructionId());
        response.setBankReportStatus(batch.getBankReportStatus());
        response.setBankReportReason(batch.getBankReportReason());
        response.setBankReportJson(batch.getBankReportJson());
        response.setBankQueuedAt(batch.getBankQueuedAt());
        response.setBankSubmittedAt(batch.getBankSubmittedAt());
        response.setBankReportRetrievedAt(batch.getBankReportRetrievedAt());
        response.setCreatedAt(batch.getCreatedAt());
        List<PayrollPaymentItemResponse> itemResponses = new ArrayList<>();
        for (PayrollPaymentItemEntity item : items) {
            PayrollPaymentItemResponse value = new PayrollPaymentItemResponse();
            value.setId(item.getId());
            value.setBatchId(item.getBatchId());
            value.setEmployeeId(item.getEmployeeId());
            value.setEmployeeNo(item.getEmployeeNo());
            value.setEmployeeName(item.getEmployeeName());
            value.setBankName(item.getBankName());
            value.setBranchCode(item.getBranchCode());
            value.setAccountNo(item.getAccountNo());
            value.setAccountType(item.getAccountType());
            value.setAccountHolderName(item.getAccountHolderName());
            value.setAmountCents(item.getAmountCents());
            value.setPaymentReference(item.getPaymentReference());
            value.setSalaryReference(item.getSalaryReference());
            value.setStatus(item.getStatus());
            value.setExcluded(item.getExcluded());
            value.setExclusionReason(item.getExclusionReason());
            value.setFailureReason(item.getFailureReason());
            itemResponses.add(value);
        }
        response.setItems(itemResponses);
        return response;
    }

    private PayrollPaymentBatchEntity requireBatch(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll payment batch not found: " + batchId));
    }

    private void createAudit(String batchId, String action, String oldStatus, String newStatus, String message, String userId) {
        PayrollPaymentBatchAuditEntity audit = new PayrollPaymentBatchAuditEntity();
        audit.setBatchId(batchId);
        audit.setAction(action);
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setMessage(message);
        audit.setCreatedBy(userId);
        auditRepository.save(audit);
    }

    private void validateCreateRequest(PayrollPaymentBatchCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("Payroll batch is required");
        if (!hasText(request.getBatchNo())) throw new IllegalArgumentException("Batch number is required");
        validatePayPeriod(request.getPayPeriod());
        if (request.getPaymentDate() == null) throw new IllegalArgumentException("Payment date is required");
    }

    private void validateCopyRequest(PayrollPaymentBatchCopyRequest request) {
        if (request == null || !hasText(request.getBatchNo())) throw new IllegalArgumentException("Batch number is required");
        validatePayPeriod(request.getPayPeriod());
        if (request.getPaymentDate() == null) throw new IllegalArgumentException("Payment date is required");
    }

    private void validatePayPeriod(String value) {
        if (!hasText(value) || !value.matches("\\d{6}")) {
            throw new IllegalArgumentException("Pay period must be in YYYYMM format");
        }
    }

    private String resolveProviderStatus(BankPaymentResponse report) {
        if (report == null) return "PENDING";
        List<String> transactionStatuses = new ArrayList<>();
        List<String> paymentStatuses = new ArrayList<>();
        if (report.getOriginalPaymentInformation() != null) {
            for (OriginalPaymentInformation payment : report.getOriginalPaymentInformation()) {
                if (hasText(payment.getPaymentInformationStatus())) {
                    paymentStatuses.add(payment.getPaymentInformationStatus());
                }
                if (payment.getTransactionInfoAndStatus() != null) {
                    for (TransactionInfoAndStatus transaction : payment.getTransactionInfoAndStatus()) {
                        if (hasText(transaction.getTransactionStatus())) {
                            transactionStatuses.add(transaction.getTransactionStatus());
                        }
                    }
                }
            }
        }
        if (!transactionStatuses.isEmpty()) return aggregateStatuses(transactionStatuses);
        if (!paymentStatuses.isEmpty()) return aggregateStatuses(paymentStatuses);
        return hasText(report.getGroupStatus()) ? report.getGroupStatus() : "PENDING";
    }

    private String aggregateStatuses(List<String> statuses) {
        for (String status : statuses) if (isFailedBankStatus(status)) return status;
        if (statuses.stream().allMatch(this::isSuccessfulBankStatus)) {
            return statuses.get(statuses.size() - 1);
        }
        return statuses.stream()
                .filter(status -> !isSuccessfulBankStatus(status))
                .findFirst()
                .orElse(statuses.get(statuses.size() - 1));
    }

    private String resolveProviderReason(BankPaymentResponse report) {
        if (report == null) return "No payment report returned by FNB";
        String value = reasonFrom(report.getStatusReasonInformation());
        if (value != null) return value;
        if (report.getOriginalPaymentInformation() != null) {
            for (OriginalPaymentInformation payment : report.getOriginalPaymentInformation()) {
                value = reasonFrom(payment.getStatusReasonInformation());
                if (value != null) return value;
                if (payment.getTransactionInfoAndStatus() != null) {
                    for (TransactionInfoAndStatus transaction : payment.getTransactionInfoAndStatus()) {
                        value = reasonFrom(transaction.getStatusReasonInformation());
                        if (value != null) return value;
                    }
                }
            }
        }
        return "FNB returned status " + resolveProviderStatus(report);
    }

    private String reasonFrom(List<StatusReasonInformation> reasons) {
        if (reasons == null) return null;
        for (StatusReasonInformation reason : reasons) {
            if (reason == null) continue;
            if (hasText(reason.getAdditionalInformation())) return reason.getAdditionalInformation();
            if (hasText(reason.getReason())) return reason.getReason();
        }
        return null;
    }

    private boolean isSuccessfulBankStatus(String status) {
        return hasText(status) && Set.of("ACSC", "ACCC", "COMPLETED", "COMPLETE", "SUCCESS", "SUCCEEDED", "PAID")
                .contains(status.trim().toUpperCase());
    }

    private boolean isFailedBankStatus(String status) {
        return hasText(status) && Set.of("RJCT", "REJECTED", "FAILED", "FAILURE", "CANCELLED", "CANCELED", "CANC")
                .contains(status.trim().toUpperCase());
    }

    private String user(String value) { return hasText(value) ? value : "SYSTEM"; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
