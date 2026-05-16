package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.payment.*;
import za.co.mawa.bes.dto.v2.payment.*;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestStatusHistoryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service(value = "PaymentRequestServiceV2")
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentRequestStatusHistoryRepository statusHistoryRepository;

    public PaymentRequestService(
            PaymentRequestRepository paymentRequestRepository,
            PaymentRequestStatusHistoryRepository statusHistoryRepository
    ) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public PaymentRequestResponse create(PaymentRequestCreateRequest request, String currentUser) {
        validateCreateRequest(request);

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
        entity.setNotes(request.getNotes());
        entity.setRequestedPaymentDate(request.getRequestedPaymentDate());
        entity.setStatus(PaymentRequestStatus.DRAFT);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        PaymentRequestEntity saved = paymentRequestRepository.save(entity);
        saveHistory(saved.getId(), null, PaymentRequestStatus.DRAFT, "Payment request created", currentUser);
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
        return toResponse(saved);
    }

    public List<PaymentRequestStatusHistoryEntity> getHistory(String id) {
        findById(id);
        return statusHistoryRepository.findByPaymentRequestIdOrderByChangedAtAsc(id);
    }

    private PaymentRequestEntity findById(String id) {
        return paymentRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found: " + id));
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
        return "PAY-" + System.currentTimeMillis();
    }

    private String defaultCurrency(String currency) {
        return currency == null || currency.isBlank() ? "ZAR" : currency;
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
                .setPaidDate(entity.getPaidDate())
                .setPaidReference(entity.getPaidReference())
                .setPaidBy(entity.getPaidBy())
                .setCreatedAt(entity.getCreatedAt())
                .setCreatedBy(entity.getCreatedBy())
                .setUpdatedAt(entity.getUpdatedAt())
                .setUpdatedBy(entity.getUpdatedBy());
    }
}
