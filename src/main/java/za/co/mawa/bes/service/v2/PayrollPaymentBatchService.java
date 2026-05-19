package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchAuditEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;
import za.co.mawa.bes.enums.payroll.PayrollPaymentItemStatus;
import za.co.mawa.bes.repository.v2.PayrollPaymentBatchAuditRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentBatchRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentItemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollPaymentBatchService {

    private final PayrollPaymentBatchRepository batchRepository;
    private final PayrollPaymentItemRepository itemRepository;
    private final PayrollPaymentBatchAuditRepository auditRepository;

    @Transactional
    public PayrollPaymentBatchResponse createBatch(PayrollPaymentBatchCreateRequest request, String userId) {
        validateCreateRequest(request);

        batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
            throw new RuntimeException("Payroll payment batch number already exists: " + request.getBatchNo());
        });

        PayrollPaymentBatchEntity batch = new PayrollPaymentBatchEntity();
        batch.setBatchNo(request.getBatchNo());
        batch.setDescription(request.getDescription());
        batch.setPayPeriod(request.getPayPeriod());
        batch.setPaymentDate(request.getPaymentDate());
        batch.setNotes(request.getNotes());
        batch.setStatus(PayrollPaymentBatchStatus.DRAFT);
        batch.setCreatedBy(userId);

        PayrollPaymentBatchEntity savedBatch = batchRepository.save(batch);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (PayrollPaymentItemRequest itemRequest : request.getItems()) {
                PayrollPaymentItemEntity item = toItemEntity(itemRequest);
                item.setBatchId(savedBatch.getId());
                item.setCreatedBy(userId);
                itemRepository.save(item);
            }
        }

        recalculateBatchTotals(savedBatch.getId());

        createAudit(
                savedBatch.getId(),
                "CREATED",
                null,
                PayrollPaymentBatchStatus.DRAFT.name(),
                "Payroll payment batch created",
                userId
        );

        return getBatch(savedBatch.getId());
    }

    @Transactional
    public PayrollPaymentBatchResponse editBatch(String batchId, PayrollPaymentBatchEditRequest request, String userId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT payroll payment batches can be edited");
        }

        // Update batch details
        if (request.getBatchNo() != null && !request.getBatchNo().isBlank()) {
            batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
                if (!existing.getId().equals(batchId)) {
                    throw new RuntimeException("Payroll payment batch number already exists: " + request.getBatchNo());
                }
            });
            batch.setBatchNo(request.getBatchNo());
        }

        if (request.getDescription() != null) {
            batch.setDescription(request.getDescription());
        }

        if (request.getPayPeriod() != null && !request.getPayPeriod().isBlank()) {
            if (!request.getPayPeriod().matches("\\d{6}")) {
                throw new RuntimeException("Pay period must be in YYYYMM format");
            }
            batch.setPayPeriod(request.getPayPeriod());
        }

        if (request.getPaymentDate() != null) {
            batch.setPaymentDate(request.getPaymentDate());
        }

        if (request.getNotes() != null) {
            batch.setNotes(request.getNotes());
        }

        batch.setUpdatedBy(userId);
        batchRepository.save(batch);

        // Edit items in the batch, if provided
        if (request.getItems() != null) {
            for (PayrollPaymentItemEditRequest itemRequest : request.getItems()) {
                PayrollPaymentItemEntity item = itemRepository.findById(itemRequest.getId())
                        .orElseThrow(() -> new RuntimeException("Payroll payment item not found: " + itemRequest.getId()));

                if (!item.getBatchId().equals(batchId)) {
                    throw new RuntimeException("Item does not belong to this payroll payment batch");
                }

                // Update item fields if present
                if (itemRequest.getEmployeeName() != null) {
                    item.setEmployeeName(itemRequest.getEmployeeName());
                }
                if (itemRequest.getBankName() != null) {
                    item.setBankName(itemRequest.getBankName());
                }
                if (itemRequest.getBranchCode() != null) {
                    item.setBranchCode(itemRequest.getBranchCode());
                }
                if (itemRequest.getAccountNo() != null) {
                    item.setAccountNo(itemRequest.getAccountNo());
                }
                if (itemRequest.getAmountCents() != null) {
                    if (itemRequest.getAmountCents() <= 0) {
                        throw new RuntimeException("Item amount must be greater than zero");
                    }
                    item.setAmountCents(itemRequest.getAmountCents());
                }

                item.setUpdatedBy(userId);
                itemRepository.save(item);
            }
        }

        recalculateBatchTotals(batch.getId());

        createAudit(
                batchId,
                "EDITED",
                null,
                batch.getStatus().name(),
                "Payroll payment batch and items edited",
                userId
        );

        return getBatch(batchId);
    }

    @Transactional
    public PayrollPaymentBatchResponse copyPreviousBatch(
            String sourceBatchId,
            PayrollPaymentBatchCopyRequest request,
            String userId
    ) {
        PayrollPaymentBatchEntity sourceBatch = batchRepository.findById(sourceBatchId)
                .orElseThrow(() -> new RuntimeException("Source payroll batch not found: " + sourceBatchId));

        validateCopyRequest(request);

        batchRepository.findByBatchNo(request.getBatchNo()).ifPresent(existing -> {
            throw new RuntimeException("Payroll payment batch number already exists: " + request.getBatchNo());
        });

        PayrollPaymentBatchEntity newBatch = new PayrollPaymentBatchEntity();
        newBatch.setBatchNo(request.getBatchNo());
        newBatch.setDescription(request.getDescription());
        newBatch.setPayPeriod(request.getPayPeriod());
        newBatch.setPaymentDate(request.getPaymentDate());
        newBatch.setSourceBatchId(sourceBatch.getId());
        newBatch.setStatus(PayrollPaymentBatchStatus.DRAFT);
        newBatch.setNotes(request.getNotes());
        newBatch.setCreatedBy(userId);

        PayrollPaymentBatchEntity savedBatch = batchRepository.save(newBatch);

        List<PayrollPaymentItemEntity> sourceItems;

        if (Boolean.TRUE.equals(request.getCopyExcludedItems())) {
            sourceItems = itemRepository.findByBatchIdOrderByEmployeeNameAsc(sourceBatchId);
        } else {
            sourceItems = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(sourceBatchId);
        }

        for (PayrollPaymentItemEntity sourceItem : sourceItems) {
            PayrollPaymentItemEntity copiedItem = new PayrollPaymentItemEntity();

            copiedItem.setBatchId(savedBatch.getId());
            copiedItem.setEmployeeId(sourceItem.getEmployeeId());
            copiedItem.setEmployeeNo(sourceItem.getEmployeeNo());
            copiedItem.setEmployeeName(sourceItem.getEmployeeName());

            copiedItem.setBankName(sourceItem.getBankName());
            copiedItem.setBranchCode(sourceItem.getBranchCode());
            copiedItem.setAccountNo(sourceItem.getAccountNo());
            copiedItem.setAccountType(sourceItem.getAccountType());
            copiedItem.setAccountHolderName(sourceItem.getAccountHolderName());

            copiedItem.setAmountCents(sourceItem.getAmountCents());
            copiedItem.setPaymentReference(sourceItem.getPaymentReference());
            copiedItem.setSalaryReference(sourceItem.getSalaryReference());

            copiedItem.setStatus(PayrollPaymentItemStatus.PENDING);
            copiedItem.setExcluded(false);
            copiedItem.setExclusionReason(null);
            copiedItem.setFailureReason(null);

            copiedItem.setCreatedBy(userId);

            itemRepository.save(copiedItem);
        }

        recalculateBatchTotals(savedBatch.getId());

        createAudit(
                savedBatch.getId(),
                "COPIED",
                null,
                PayrollPaymentBatchStatus.DRAFT.name(),
                "Payroll payment batch copied from batch " + sourceBatch.getBatchNo(),
                userId
        );

        return getBatch(savedBatch.getId());
    }

    public PayrollPaymentBatchResponse getBatch(String batchId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        List<PayrollPaymentItemEntity> items = itemRepository.findByBatchIdOrderByEmployeeNameAsc(batchId);

        return toBatchResponse(batch, items);
    }

    public List<PayrollPaymentBatchResponse> getByPayPeriod(String payPeriod) {
        return batchRepository.findByPayPeriodOrderByCreatedAtDesc(payPeriod)
                .stream()
                .map(batch -> {
                    List<PayrollPaymentItemEntity> items =
                            itemRepository.findByBatchIdOrderByEmployeeNameAsc(batch.getId());
                    return toBatchResponse(batch, items);
                })
                .toList();
    }

    @Transactional
    public PayrollPaymentBatchResponse addItem(
            String batchId,
            PayrollPaymentItemRequest request,
            String userId
    ) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new RuntimeException("Items can only be added to a DRAFT payroll payment batch");
        }

        PayrollPaymentItemEntity item = toItemEntity(request);
        item.setBatchId(batchId);
        item.setCreatedBy(userId);

        itemRepository.save(item);

        recalculateBatchTotals(batchId);

        createAudit(
                batchId,
                "ITEM_ADDED",
                batch.getStatus().name(),
                batch.getStatus().name(),
                "Payroll payment item added for " + request.getEmployeeName(),
                userId
        );

        return getBatch(batchId);
    }

    @Transactional
    public PayrollPaymentBatchResponse approveBatch(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        if (batch.getStatus() != PayrollPaymentBatchStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT payroll payment batches can be approved");
        }

        List<PayrollPaymentItemEntity> items = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);

        if (items.isEmpty()) {
            throw new RuntimeException("Cannot approve payroll payment batch with no payable items");
        }

        PayrollPaymentBatchStatus oldStatus = batch.getStatus();

        batch.setStatus(PayrollPaymentBatchStatus.APPROVED);
        batch.setUpdatedBy(userId);
        batchRepository.save(batch);

        createAudit(
                batchId,
                "APPROVED",
                oldStatus.name(),
                PayrollPaymentBatchStatus.APPROVED.name(),
                "Payroll payment batch approved",
                userId
        );

        return getBatch(batchId);
    }

    @Transactional
    public PayrollPaymentBatchResponse cancelBatch(String batchId, String userId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        if (batch.getStatus() == PayrollPaymentBatchStatus.PAID) {
            throw new RuntimeException("Paid payroll payment batch cannot be cancelled");
        }

        PayrollPaymentBatchStatus oldStatus = batch.getStatus();

        batch.setStatus(PayrollPaymentBatchStatus.CANCELLED);
        batch.setUpdatedBy(userId);
        batchRepository.save(batch);

        createAudit(
                batchId,
                "CANCELLED",
                oldStatus.name(),
                PayrollPaymentBatchStatus.CANCELLED.name(),
                "Payroll payment batch cancelled",
                userId
        );

        return getBatch(batchId);
    }

    @Transactional
    public void recalculateBatchTotals(String batchId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Payroll payment batch not found: " + batchId));

        List<PayrollPaymentItemEntity> payableItems =
                itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);

        int totalEmployees = payableItems.size();

        long totalAmountCents = payableItems.stream()
                .mapToLong(item -> item.getAmountCents() == null ? 0L : item.getAmountCents())
                .sum();

        batch.setTotalEmployees(totalEmployees);
        batch.setTotalAmountCents(totalAmountCents);

        batchRepository.save(batch);
    }

    private PayrollPaymentItemEntity toItemEntity(PayrollPaymentItemRequest request) {
        if (request.getEmployeeName() == null || request.getEmployeeName().isBlank()) {
            throw new RuntimeException("Employee name is required");
        }

        if (request.getAccountNo() == null || request.getAccountNo().isBlank()) {
            throw new RuntimeException("Account number is required");
        }

        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        PayrollPaymentItemEntity item = new PayrollPaymentItemEntity();

        item.setEmployeeId(request.getEmployeeId());
        item.setEmployeeNo(request.getEmployeeNo());
        item.setEmployeeName(request.getEmployeeName());

        item.setBankName(request.getBankName());
        item.setBranchCode(request.getBranchCode());
        item.setAccountNo(request.getAccountNo());
        item.setAccountType(request.getAccountType());
        item.setAccountHolderName(request.getAccountHolderName());

        item.setAmountCents(request.getAmountCents());
        item.setPaymentReference(request.getPaymentReference());
        item.setSalaryReference(request.getSalaryReference());

        item.setStatus(PayrollPaymentItemStatus.PENDING);
        item.setExcluded(false);

        return item;
    }

    private PayrollPaymentBatchResponse toBatchResponse(
            PayrollPaymentBatchEntity batch,
            List<PayrollPaymentItemEntity> items
    ) {
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
        response.setCreatedAt(batch.getCreatedAt());

        List<PayrollPaymentItemResponse> itemResponses = new ArrayList<>();

        for (PayrollPaymentItemEntity item : items) {
            PayrollPaymentItemResponse itemResponse = new PayrollPaymentItemResponse();

            itemResponse.setId(item.getId());
            itemResponse.setBatchId(item.getBatchId());
            itemResponse.setEmployeeId(item.getEmployeeId());
            itemResponse.setEmployeeNo(item.getEmployeeNo());
            itemResponse.setEmployeeName(item.getEmployeeName());

            itemResponse.setBankName(item.getBankName());
            itemResponse.setBranchCode(item.getBranchCode());
            itemResponse.setAccountNo(item.getAccountNo());
            itemResponse.setAccountType(item.getAccountType());
            itemResponse.setAccountHolderName(item.getAccountHolderName());

            itemResponse.setAmountCents(item.getAmountCents());
            itemResponse.setPaymentReference(item.getPaymentReference());
            itemResponse.setSalaryReference(item.getSalaryReference());

            itemResponse.setStatus(item.getStatus());
            itemResponse.setExcluded(item.getExcluded());
            itemResponse.setExclusionReason(item.getExclusionReason());
            itemResponse.setFailureReason(item.getFailureReason());

            itemResponses.add(itemResponse);
        }

        response.setItems(itemResponses);

        return response;
    }

    private void createAudit(
            String batchId,
            String action,
            String oldStatus,
            String newStatus,
            String message,
            String userId
    ) {
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
        if (request.getBatchNo() == null || request.getBatchNo().isBlank()) {
            throw new RuntimeException("Batch number is required");
        }

        if (request.getPayPeriod() == null || request.getPayPeriod().isBlank()) {
            throw new RuntimeException("Pay period is required");
        }

        if (!request.getPayPeriod().matches("\\d{6}")) {
            throw new RuntimeException("Pay period must be in YYYYMM format");
        }

        if (request.getPaymentDate() == null) {
            throw new RuntimeException("Payment date is required");
        }
    }

    private void validateCopyRequest(PayrollPaymentBatchCopyRequest request) {
        if (request.getBatchNo() == null || request.getBatchNo().isBlank()) {
            throw new RuntimeException("Batch number is required");
        }

        if (request.getPayPeriod() == null || request.getPayPeriod().isBlank()) {
            throw new RuntimeException("Pay period is required");
        }

        if (!request.getPayPeriod().matches("\\d{6}")) {
            throw new RuntimeException("Pay period must be in YYYYMM format");
        }

        if (request.getPaymentDate() == null) {
            throw new RuntimeException("Payment date is required");
        }
    }
}
