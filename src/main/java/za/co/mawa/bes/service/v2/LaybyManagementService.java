package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.layby.LaybyDtos;
import za.co.mawa.bes.dto.v2.payment.MarkPaymentRequestPaidRequest;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestCreateRequest;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.dto.v2.stock.StockDtos;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.SyncStatus;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.InvoiceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaybyManagementService {

    private static final String TERMS_VERSION = "CPA-LAYBY-V1";
    private static final List<String> SUPPORTED_FREQUENCIES = List.of("WEEKLY", "FORTNIGHTLY", "MONTHLY", "ONCE");

    private final JdbcTemplate jdbcTemplate;
    private final StockOperationsService stockOperationsService;
    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;
    private final OnlineCashupService onlineCashupService;
    private final NumberAllocationService numberAllocationService;
    private final InvoiceService invoiceService;
    private final ApprovalService approvalService;
    private final PaymentRequestService paymentRequestService;
    private final AttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getConfiguration() {
        ensureConfiguration();
        return jdbcTemplate.queryForMap("SELECT * FROM layby_configuration WHERE id=1");
    }

    @Transactional
    public Map<String, Object> updateConfiguration(LaybyDtos.ConfigurationRequest request, String actor) {
        ensureConfiguration();
        if (request == null) throw new IllegalArgumentException("Configuration is required");

        Map<String, Object> current = getConfiguration();
        String frequency = normalizeFrequency(firstNonBlank(request.getDefaultPaymentFrequency(), text(current.get("default_payment_frequency"))));
        int duration = request.getDefaultDurationMonths() == null ? integer(current.get("default_duration_months")) : request.getDefaultDurationMonths();
        BigDecimal depositPercent = request.getMinimumDepositPercent() == null ? decimal(current.get("minimum_deposit_percent")) : request.getMinimumDepositPercent();
        BigDecimal penaltyPercent = request.getCancellationPenaltyPercent() == null ? decimal(current.get("cancellation_penalty_percent")) : request.getCancellationPenaltyPercent();
        int graceDays = request.getDefaultGraceBusinessDays() == null ? integer(current.get("default_grace_business_days")) : request.getDefaultGraceBusinessDays();

        if (duration < 1 || duration > 60) throw new IllegalArgumentException("Default layby duration must be between 1 and 60 months");
        requirePercent(depositPercent, 0, 100, "minimumDepositPercent");
        requirePercent(penaltyPercent, 0, 1, "cancellationPenaltyPercent");
        if (graceDays < 60) throw new IllegalArgumentException("Layby grace period cannot be less than 60 business days");

        jdbcTemplate.update("""
                UPDATE layby_configuration
                   SET enabled=?, default_payment_frequency=?, default_duration_months=?,
                       deposit_required=?, minimum_deposit_percent=?, cancellation_penalty_percent=?,
                       default_grace_business_days=?, require_cancellation_approval=?, require_refund_approval=?,
                       create_refund_payment_request_on_cancellation=?,
                       automatically_reserve_stock=?, allow_stock_short_layby=?, updated_at=CURRENT_TIMESTAMP, updated_by=?
                 WHERE id=1
                """,
                bool(request.getEnabled(), current.get("enabled")), frequency, duration,
                bool(request.getDepositRequired(), current.get("deposit_required")), depositPercent, penaltyPercent,
                graceDays, bool(request.getRequireCancellationApproval(), current.get("require_cancellation_approval")),
                bool(request.getRequireRefundApproval(), current.get("require_refund_approval")),
                bool(request.getCreateRefundPaymentRequestOnCancellation(), current.get("create_refund_payment_request_on_cancellation")),
                bool(request.getAutomaticallyReserveStock(), current.get("automatically_reserve_stock")),
                bool(request.getAllowStockShortLayby(), current.get("allow_stock_short_layby")), actor(actor));
        return getConfiguration();
    }

    @Transactional
    public Map<String, Object> create(LaybyDtos.CreateLaybyRequest request, String fallbackActor) {
        if (request == null) throw new IllegalArgumentException("Layby request is required");
        Map<String, Object> config = getConfiguration();
        if (!booleanValue(config.get("enabled"))) throw new IllegalStateException("Laybys are disabled for this tenant");
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new IllegalArgumentException("Layby terms must be accepted before the agreement can be created");
        }

        String actor = actor(firstNonBlank(request.getTermsAcceptedBy(), fallbackActor));
        Map<String, Object> salesOrder;
        String quotationId = clean(request.getQuotationId());
        String customerId;

        if (quotationId != null) {
            Map<String, Object> quotation = stockOperationsService.getQuotation(quotationId);
            String quotationStatus = upper(quotation.get("status"));
            if (!"ACCEPTED".equals(quotationStatus)) {
                throw new IllegalStateException("Only an accepted quotation can be converted to a layby");
            }
            customerId = firstNonBlank(clean(request.getCustomerPartnerId()), text(quotation.get("customer_partner_id")));
            requireCustomer(customerId);
            StockDtos.ConvertQuotationRequest conversion = new StockDtos.ConvertQuotationRequest();
            conversion.setWarehouseId(clean(request.getWarehouseId()));
            conversion.setRequestedDeliveryDate(request.getRequestedDeliveryDate());
            conversion.setNotes(firstNonBlank(request.getNotes(), "Created for layby"));
            salesOrder = stockOperationsService.convertQuotationToSalesOrder(quotationId, conversion, actor);
        } else {
            customerId = clean(request.getCustomerPartnerId());
            requireCustomer(customerId);
            if (request.getLines() == null || request.getLines().isEmpty()) {
                throw new IllegalArgumentException("At least one product is required for a layby");
            }
            StockDtos.SalesOrderRequest salesRequest = new StockDtos.SalesOrderRequest();
            salesRequest.setCustomerPartnerId(customerId);
            salesRequest.setCustomerReference(clean(request.getCustomerReference()));
            salesRequest.setWarehouseId(clean(request.getWarehouseId()));
            salesRequest.setRequestedDeliveryDate(request.getRequestedDeliveryDate() == null ? null : request.getRequestedDeliveryDate().toString());
            salesRequest.setCurrency(firstNonBlank(request.getCurrency(), "ZAR"));
            salesRequest.setNotes(firstNonBlank(request.getNotes(), "Created for layby"));
            for (LaybyDtos.LineRequest line : request.getLines()) {
                StockDtos.SalesOrderLineRequest soLine = new StockDtos.SalesOrderLineRequest();
                soLine.setProductId(clean(line.getProductId()));
                soLine.setProductCode(clean(line.getProductCode()));
                soLine.setDescription(clean(line.getDescription()));
                soLine.setQuantity(line.getQuantity());
                soLine.setUom(firstNonBlank(line.getUom(), "EA"));
                soLine.setUnitPrice(line.getUnitPrice());
                soLine.setTaxRate(line.getTaxRate());
                soLine.setNotes(clean(line.getNotes()));
                salesRequest.getLines().add(soLine);
            }
            salesOrder = stockOperationsService.createSalesOrder(salesRequest, actor);
        }

        String salesOrderId = text(salesOrder.get("id"));
        requireWarehouseForStockControlledLines(salesOrderId, text(salesOrder.get("warehouse_id")));

        long totalCents = cents(decimal(salesOrder.get("total_amount")));
        if (totalCents <= 0) throw new IllegalStateException("Layby total must be greater than zero");

        String frequency = normalizeFrequency(firstNonBlank(request.getPaymentFrequency(), text(config.get("default_payment_frequency"))));
        int count = request.getInstallmentCount() == null || request.getInstallmentCount() <= 0
                ? defaultInstallmentCount(frequency, integer(config.get("default_duration_months")))
                : request.getInstallmentCount();
        if (count < 1 || count > 260) throw new IllegalArgumentException("installmentCount must be between 1 and 260");

        BigDecimal minimumDepositPercent = decimal(config.get("minimum_deposit_percent"));
        long configuredMinimumDeposit = booleanValue(config.get("deposit_required"))
                ? percentOf(totalCents, minimumDepositPercent) : 0L;
        long agreedDeposit = request.getDepositCents() == null ? configuredMinimumDeposit : request.getDepositCents();
        if (agreedDeposit < configuredMinimumDeposit) {
            throw new IllegalArgumentException("Deposit cannot be lower than the configured minimum of " + configuredMinimumDeposit + " cents");
        }
        if (agreedDeposit < 0 || agreedDeposit > totalCents) throw new IllegalArgumentException("Invalid deposit amount");

        LocalDate firstDue = request.getFirstInstallmentDate();
        if (firstDue == null) firstDue = firstDueDate(frequency, booleanValue(config.get("deposit_required")));
        LocalDate completion = dueDate(firstDue, frequency, count - 1);

        String id = uuid();
        String laybyNo = numberAllocationService.allocateNumber("LAYBY");
        jdbcTemplate.update("""
                INSERT INTO layby_agreement(
                    id, layby_no, sales_order_id, quotation_id, customer_partner_id, warehouse_id,
                    status, currency, total_cents, paid_cents, balance_cents, deposit_required_cents,
                    payment_frequency, installment_count, first_installment_date, expected_completion_date,
                    grace_business_days, cancellation_penalty_percent, terms_version, terms_accepted_at,
                    terms_accepted_by, notes, created_at, created_by, updated_at, updated_by
                ) VALUES(?,?,?,?,?,?,'DRAFT',?,?,0,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)
                """,
                id, laybyNo, salesOrderId, quotationId, customerId, text(salesOrder.get("warehouse_id")),
                firstNonBlank(request.getCurrency(), text(salesOrder.get("currency")), "ZAR"), totalCents, totalCents,
                agreedDeposit, frequency, count, Date.valueOf(firstDue), Date.valueOf(completion),
                integer(config.get("default_grace_business_days")), decimal(config.get("cancellation_penalty_percent")),
                TERMS_VERSION, actor, clean(request.getNotes()), actor, actor);

        createInstallments(id, totalCents, count, firstDue, frequency, actor);
        history(id, null, "DRAFT", "Layby created", actor);
        return get(id);
    }

    public List<Map<String, Object>> list(String status, String query, String customerPartnerId) {
        refreshArrearsStatuses();
        StringBuilder sql = new StringBuilder("""
                SELECT l.*, so.sales_order_no, p.number AS customer_no,
                       TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) AS customer_name,
                       (SELECT MIN(i.due_date) FROM layby_installment i WHERE i.layby_agreement_id=l.id AND i.balance_cents>0) AS next_due_date,
                       (SELECT COALESCE(SUM(i.balance_cents),0) FROM layby_installment i WHERE i.layby_agreement_id=l.id AND i.due_date<CURRENT_DATE AND i.balance_cents>0) AS overdue_cents
                  FROM layby_agreement l
                  JOIN sales_order so ON so.id=l.sales_order_id
                  JOIN partner p ON p.id=l.customer_partner_id
                 WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (clean(status) != null) { sql.append(" AND l.status=?"); args.add(status.trim().toUpperCase(Locale.ROOT)); }
        if (clean(customerPartnerId) != null) { sql.append(" AND l.customer_partner_id=?"); args.add(customerPartnerId.trim()); }
        if (clean(query) != null) {
            sql.append("""
                     AND (UPPER(l.layby_no) LIKE ?
                       OR UPPER(COALESCE(p.number,'')) LIKE ?
                       OR UPPER(CONCAT_WS(' ',p.name1,p.name2,p.name3)) LIKE ?
                       OR UPPER(COALESCE(so.sales_order_no,'')) LIKE ?
                       OR EXISTS (SELECT 1 FROM partner_identity pi WHERE pi.partner=p.id AND UPPER(COALESCE(pi.value,'')) LIKE ?)
                       OR EXISTS (SELECT 1 FROM partner_contact pc WHERE pc.partner=p.id AND UPPER(COALESCE(pc.value,'')) LIKE ?)
                       OR EXISTS (SELECT 1 FROM sales_order_line sol JOIN product pr ON pr.id=sol.product_id
                                   WHERE sol.sales_order_id=so.id AND (UPPER(COALESCE(pr.code,'')) LIKE ? OR UPPER(COALESCE(pr.description,'')) LIKE ?)))
                    """);
            String like = "%" + query.trim().toUpperCase(Locale.ROOT) + "%";
            for (int i = 0; i < 8; i++) args.add(like);
        }
        sql.append(" ORDER BY l.created_at DESC LIMIT 500");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        BusinessCalendar businessCalendar = loadBusinessCalendar();
        rows.forEach(row -> addOperationalFlags(row, businessCalendar));
        return rows;
    }

    public Map<String, Object> get(String id) {
        refreshArrearsStatus(id);
        Map<String, Object> result = new LinkedHashMap<>(jdbcTemplate.queryForMap("""
                SELECT l.*, so.sales_order_no, so.status AS sales_order_status, p.number AS customer_no,
                       TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) AS customer_name
                  FROM layby_agreement l
                  JOIN sales_order so ON so.id=l.sales_order_id
                  JOIN partner p ON p.id=l.customer_partner_id
                 WHERE l.id=?
                """, id));
        result.put("installments", jdbcTemplate.queryForList("SELECT * FROM layby_installment WHERE layby_agreement_id=? ORDER BY installment_no", id));
        result.put("payments", jdbcTemplate.queryForList("""
                SELECT r.id receipt_id, r.receipt_no, r.receipt_date, r.payment_method, r.total_amount_cents,
                       r.status receipt_status, a.amount_cents allocated_amount_cents, r.created_by
                  FROM receipt_allocation a
                  JOIN receipt r ON r.id=a.receipt_id
                 WHERE a.allocation_type='LAYBY' AND a.reference_id=?
                 ORDER BY r.receipt_date DESC, r.created_at DESC
                """, id));
        List<Map<String, Object>> refunds = jdbcTemplate.queryForList("""
                SELECT lr.*, pr.request_no AS payment_request_no, pr.status AS payment_request_status,
                       pr.approval_request_id AS payment_request_linked_approval_id
                  FROM layby_refund lr
                  LEFT JOIN payment_request pr ON pr.id=lr.payment_request_id
                 WHERE lr.layby_agreement_id=?
                """, id);
        if (refunds.isEmpty()) {
            result.put("refund", null);
        } else {
            Map<String, Object> refund = new LinkedHashMap<>(refunds.get(0));
            String paymentRequestStatus = upper(refund.get("payment_request_status"));
            if ("PENDING_APPROVAL".equals(paymentRequestStatus)) refund.put("status", "PENDING_APPROVAL");
            else if (List.of("APPROVED", "QUEUED_FOR_PAYMENT", "PROCESSED").contains(paymentRequestStatus)) refund.put("status", "APPROVED");
            else if ("PAID".equals(paymentRequestStatus)) refund.put("status", "PAID");
            else if ("REJECTED".equals(paymentRequestStatus)) refund.put("status", "REJECTED");
            else if ("CANCELLED".equals(paymentRequestStatus)) refund.put("status", "CANCELLED");
            result.put("refund", refund);
        }
        result.put("statusHistory", jdbcTemplate.queryForList("SELECT * FROM layby_status_history WHERE layby_agreement_id=? ORDER BY changed_at DESC", id));
        result.put("salesOrder", stockOperationsService.getSalesOrder(text(result.get("sales_order_id"))));
        addOperationalFlags(result, loadBusinessCalendar());
        return result;
    }

    @Transactional
    public Map<String, Object> activate(String id, String actorValue) {
        Map<String, Object> agreement = lockAgreement(id);
        String status = upper(agreement.get("status"));
        if (!"DRAFT".equals(status)) throw new IllegalStateException("Only a draft layby can be activated");
        long paid = longValue(agreement.get("paid_cents"));
        long deposit = longValue(agreement.get("deposit_required_cents"));
        if (paid < deposit) throw new IllegalStateException("The required deposit must be paid before the layby can be activated");

        Map<String, Object> config = getConfiguration();
        if (booleanValue(config.get("automatically_reserve_stock"))) {
            Map<String, Object> order = stockOperationsService.reserveSalesOrderForLayby(text(agreement.get("sales_order_id")), actor(actorValue));
            if (!booleanValue(config.get("allow_stock_short_layby"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("lines");
                boolean shortStock = lines.stream().anyMatch(line -> decimal(line.get("reserved_qty")).compareTo(decimal(line.get("quantity"))) < 0);
                if (shortStock) throw new IllegalStateException("Layby cannot be activated because all stock could not be reserved");
            }
        }
        String activatedStatus = longValue(agreement.get("balance_cents")) == 0 ? "PAID_UP" : paymentStatus(id);
        setStatus(id, activatedStatus, "Layby activated", actor(actorValue));
        return get(id);
    }

    @Transactional
    public PaymentBatchResponseDto capturePayment(String id, LaybyDtos.PaymentRequest request, String fallbackActor) {
        if (request == null || request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (clean(request.getPaymentMethod()) == null) throw new IllegalArgumentException("paymentMethod is required");

        Map<String, Object> agreement = lockAgreement(id);
        String status = upper(agreement.get("status"));
        if (!List.of("DRAFT", "ACTIVE", "IN_ARREARS").contains(status)) {
            throw new IllegalStateException("Payments can only be captured for draft or active laybys. Current status: " + status);
        }
        long balance = longValue(agreement.get("balance_cents"));
        if (balance <= 0) throw new IllegalStateException("Layby has no outstanding balance");
        if (request.getAmountCents() > balance) throw new IllegalArgumentException("Payment amount exceeds layby balance");

        String actor = actor(firstNonBlank(request.getCreatedBy(), fallbackActor));
        LocalDate paymentDate = request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate();
        LocalDateTime paymentDateTime = paymentDate.atStartOfDay();
        String method = request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        String deviceId = firstNonBlank(request.getDeviceId(), "ERP-ONLINE");

        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(numberAllocationService.allocateNumber("PAYMENT_BATCH"));
        batch.setSourceType(ReceiptSourceType.LAYBY);
        batch.setReceivedFromPartnerId(text(agreement.get("customer_partner_id")));
        batch.setPaymentMethod(method);
        batch.setTotalAmountCents(request.getAmountCents());
        batch.setPaymentDate(paymentDateTime);
        batch.setLocation(clean(request.getLocation()));
        batch.setEmployeeResponsible(clean(request.getEmployeeResponsible()));
        batch.setDeviceId(deviceId);
        batch.setTerminalId(clean(request.getTerminalId()));
        batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setNotes(clean(request.getNotes()));
        batch.setCreatedAt(LocalDateTime.now());
        batch.setCreatedBy(actor);
        batch = paymentBatchRepository.save(batch);

        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(numberAllocationService.allocateNumber("RECEIPT"));
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.LAYBY);
        receipt.setReceivedFromPartnerId(text(agreement.get("customer_partner_id")));
        receipt.setReceiptDate(paymentDateTime);
        receipt.setPaymentMethod(method);
        receipt.setTotalAmountCents(request.getAmountCents());
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setSyncStatus(SyncStatus.SYNCED);
        receipt.setLocation(clean(request.getLocation()));
        receipt.setEmployeeResponsible(clean(request.getEmployeeResponsible()));
        receipt.setDeviceId(deviceId);
        receipt.setTerminalId(clean(request.getTerminalId()));
        receipt.setCaptureSource("ERP_ONLINE");
        receipt.setCapturedBy(actor);
        receipt.setPrinted(false);
        receipt.setPrintCount(0);
        receipt.setNotes("Layby payment " + text(agreement.get("layby_no")) + (clean(request.getNotes()) == null ? "" : " - " + request.getNotes().trim()));
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(actor);
        receipt = receiptRepository.save(receipt);

        var allocation = receiptService.createAllocation(
                receipt.getId(), ReceiptAllocationType.LAYBY, id, text(agreement.get("layby_no")),
                null, null, request.getAmountCents(), actor);
        allocateToInstallments(id, receipt.getId(), request.getAmountCents(), actor);

        long newPaid = longValue(agreement.get("paid_cents")) + request.getAmountCents();
        long newBalance = Math.max(0L, longValue(agreement.get("total_cents")) - newPaid);
        String newStatus = "DRAFT".equals(status) ? "DRAFT" : (newBalance == 0 ? "PAID_UP" : paymentStatus(id));
        jdbcTemplate.update("UPDATE layby_agreement SET paid_cents=?, balance_cents=?, status=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
                newPaid, newBalance, newStatus, actor, id);
        if (!status.equals(newStatus)) history(id, status, newStatus, newBalance == 0 ? "Layby fully paid" : "Payment status refreshed", actor);

        onlineCashupService.addReceipts(batch, List.of(receipt.getId()), actor, deviceId);
        ReceiptResponseDto receiptDto = receiptMapper.toDto(receipt, List.of(allocation));
        return PaymentBatchResponseDto.builder()
                .id(batch.getId()).paymentBatchNo(batch.getPaymentBatchNo()).sourceType(batch.getSourceType())
                .receivedFromPartnerId(batch.getReceivedFromPartnerId()).paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents()).paymentDate(batch.getPaymentDate())
                .location(batch.getLocation()).employeeResponsible(batch.getEmployeeResponsible())
                .deviceId(batch.getDeviceId()).terminalId(batch.getTerminalId()).status(batch.getStatus())
                .syncStatus(batch.getSyncStatus()).notes(batch.getNotes()).createdAt(batch.getCreatedAt())
                .createdBy(batch.getCreatedBy()).receipts(List.of(receiptDto)).build();
    }

    @Transactional
    public Map<String, Object> requestCancellation(String id, LaybyDtos.CancellationRequest request, String fallbackActor) {
        if (request == null || clean(request.getReason()) == null) throw new IllegalArgumentException("Cancellation reason is required");
        String refundMethod = normalizeRefundMethod(request.getRefundMethod());
        Map<String, Object> agreement = lockAgreement(id);
        String current = upper(agreement.get("status"));
        if (!List.of("DRAFT", "ACTIVE", "IN_ARREARS", "PAID_UP").contains(current)) {
            throw new IllegalStateException("This layby cannot be cancelled from status " + current);
        }
        String actor = actor(firstNonBlank(request.getRequestedBy(), fallbackActor));
        String reasonCode = upper(request.getReasonCode());
        long penalty = ("DEATH".equals(reasonCode) || "HOSPITALISATION".equals(reasonCode) || "HOSPITALIZATION".equals(reasonCode))
                ? 0L : Math.min(longValue(agreement.get("paid_cents")), percentOf(longValue(agreement.get("total_cents")), decimal(agreement.get("cancellation_penalty_percent"))));
        long refund = Math.max(0L, longValue(agreement.get("paid_cents")) - penalty);
        Map<String, Object> configuration = getConfiguration();
        boolean approvalRequired = booleanValue(configuration.get("require_cancellation_approval"));
        String requestedStatus = approvalRequired ? "CANCELLATION_PENDING" : "CANCELLED";
        String cancellationRequestId = approvalRequired ? uuid() : null;

        jdbcTemplate.update("""
                UPDATE layby_agreement
                   SET cancellation_previous_status=?, cancellation_reason_code=?, cancellation_reason=?,
                       cancellation_requested_at=CURRENT_TIMESTAMP, cancellation_requested_by=?, cancellation_request_id=?,
                       cancellation_penalty_cents=?, refund_due_cents=?, status=?, cancellation_approval_request_id=NULL,
                       updated_at=CURRENT_TIMESTAMP, updated_by=?
                 WHERE id=?
                """, current, clean(request.getReasonCode()), request.getReason().trim(), actor, cancellationRequestId,
                penalty, refund, requestedStatus, actor, id);
        history(id, current, requestedStatus, request.getReason().trim(), actor);
        ensureRefund(id, refundMethod, actor);
        if (approvalRequired) {
            ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
            approval.setApprovalType(ApprovalType.LAYBY_CANCELLATION);
            approval.setReferenceId(cancellationRequestId);
            approval.setReferenceNo(text(agreement.get("layby_no")));
            approval.setTitle("Cancel layby " + text(agreement.get("layby_no")));
            approval.setDescription("Review the layby cancellation, penalty and customer refund before stock is released.");
            approval.setRequesterId(actor);
            approval.setPayloadJson(toJson(Map.of(
                    "laybyId", id,
                    "laybyNo", text(agreement.get("layby_no")),
                    "customerPartnerId", text(agreement.get("customer_partner_id")),
                    "paidCents", longValue(agreement.get("paid_cents")),
                    "penaltyCents", penalty,
                    "refundDueCents", refund,
                    "refundMethod", refundMethod,
                    "reasonCode", firstNonBlank(clean(request.getReasonCode()), "OTHER"),
                    "reason", request.getReason().trim())));
            ApprovalRequestResponse response = approvalService.submitForApproval(approval);
            jdbcTemplate.update("UPDATE layby_agreement SET cancellation_approval_request_id=? WHERE id=?", response.getId(), id);
        } else {
            completeCancellation(id, actor);
        }
        return get(id);
    }

    @Transactional
    public void completeCancellationApprovalByRequestId(String cancellationRequestId, boolean approved, String actorValue, String reason) {
        String id = jdbcTemplate.queryForObject(
                "SELECT id FROM layby_agreement WHERE cancellation_request_id=?", String.class, cancellationRequestId);
        if (id == null) return;
        completeCancellationApproval(id, approved, actorValue, reason);
    }

    @Transactional
    public void completeCancellationApproval(String id, boolean approved, String actorValue, String reason) {
        Map<String, Object> agreement = lockAgreement(id);
        if (!"CANCELLATION_PENDING".equals(upper(agreement.get("status")))) return;
        String actor = actor(actorValue);
        if (approved) {
            completeCancellation(id, actor);
            return;
        }
        String restored = firstNonBlank(text(agreement.get("cancellation_previous_status")), "ACTIVE").toUpperCase(Locale.ROOT);
        jdbcTemplate.update("UPDATE layby_agreement SET status=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?", restored, actor, id);
        history(id, "CANCELLATION_PENDING", restored, firstNonBlank(reason, "Cancellation rejected"), actor);
        cancelDraftRefundPaymentRequest(id, actor);
    }

    @Transactional
    public void completeRefundApprovalByReferenceId(String approvalReferenceId, boolean approved, String actorValue, String reason) {
        String laybyId = jdbcTemplate.queryForObject(
                "SELECT layby_agreement_id FROM layby_refund WHERE approval_reference_id=?", String.class, approvalReferenceId);
        if (laybyId == null) return;
        completeRefundApproval(laybyId, approved, actorValue, reason);
    }

    @Transactional
    public void completeRefundApproval(String laybyId, boolean approved, String actorValue, String reason) {
        Map<String, Object> agreement = lockAgreement(laybyId);
        if (!"CANCELLED".equals(upper(agreement.get("status")))) return;
        Map<String, Object> refund = refund(laybyId);
        if (!"PENDING_APPROVAL".equals(upper(refund.get("status")))) return;
        String actor = actor(actorValue);
        String status = approved ? "APPROVED" : "REJECTED";
        jdbcTemplate.update("UPDATE layby_refund SET status=?, approved_at=CURRENT_TIMESTAMP, approved_by=?, notes=COALESCE(?,notes) WHERE id=?",
                status, actor, clean(reason), refund.get("id"));
    }

    @Transactional
    public Map<String, Object> requestRefundApproval(String laybyId, String actorValue) {
        Map<String, Object> agreement = lockAgreement(laybyId);
        if (!"CANCELLED".equals(upper(agreement.get("status")))) {
            throw new IllegalStateException("Only a cancelled layby can have its refund resubmitted");
        }
        Map<String, Object> refund = refund(laybyId);
        String status = upper(refund.get("status"));
        if ("PENDING_APPROVAL".equals(status)) return get(laybyId);
        if (List.of("APPROVED", "PAID", "NOT_REQUIRED").contains(status)) {
            throw new IllegalStateException("This refund does not require another approval request");
        }
        if (longValue(refund.get("refund_amount_cents")) <= 0) {
            throw new IllegalStateException("There is no refund amount to approve");
        }
        if (clean(refund.get("payment_request_id")) != null) {
            throw new IllegalStateException("This layby refund uses the Customer Refund Payment Request approval process");
        }
        if (clean(refund.get("signed_cancellation_attachment_id")) == null) {
            throw new IllegalStateException("Signed cancellation form must be attached before refund approval");
        }
        String actor = actor(actorValue);
        if (!booleanValue(getConfiguration().get("require_refund_approval"))) {
            jdbcTemplate.update("UPDATE layby_refund SET status='APPROVED', approved_at=CURRENT_TIMESTAMP, approved_by=? WHERE id=?",
                    actor, refund.get("id"));
            return get(laybyId);
        }
        String approvalReferenceId = uuid();
        jdbcTemplate.update("UPDATE layby_refund SET status='PENDING_APPROVAL', approval_reference_id=?, approval_request_id=NULL, requested_at=CURRENT_TIMESTAMP, requested_by=?, approved_at=NULL, approved_by=NULL WHERE id=?",
                approvalReferenceId, actor(actor), refund.get("id"));
        submitRefundApproval(agreement, text(refund.get("id")), approvalReferenceId, longValue(refund.get("refund_amount_cents")), actor);
        return get(laybyId);
    }

    @Transactional
    public Map<String, Object> markRefundPaid(String id, LaybyDtos.RefundPaidRequest request, String fallbackActor) {
        Map<String, Object> agreement = lockAgreement(id);
        if (!"CANCELLED".equals(upper(agreement.get("status")))) throw new IllegalStateException("Layby is not cancelled");
        Map<String, Object> refund = refund(id);
        String actor = actor(firstNonBlank(request == null ? null : request.getActionBy(), fallbackActor));
        String paymentReference = clean(request == null ? null : request.getPaymentReference());
        if (paymentReference == null) throw new IllegalArgumentException("paymentReference is required when marking a layby refund paid");

        String paymentRequestId = clean(refund.get("payment_request_id"));
        if (paymentRequestId != null) {
            PaymentRequestResponse paymentRequest = paymentRequestService.getById(paymentRequestId);
            if (paymentRequest.getPaymentMethod() != PaymentMethod.CASH) {
                throw new IllegalStateException("Only Cash layby refunds are marked paid from the Layby. EFT refunds are updated by the payment process.");
            }
            if (paymentRequest.getStatus() != PaymentRequestStatus.APPROVED) {
                throw new IllegalStateException("Refund payment request must be approved before the Cash refund can be marked paid");
            }
            MarkPaymentRequestPaidRequest paid = new MarkPaymentRequestPaidRequest();
            paid.setPaidDate(LocalDate.now());
            paid.setPaidReference(paymentReference);
            paid.setComment(firstNonBlank(clean(request == null ? null : request.getNotes()),
                    "Cash customer refund completed from layby " + text(agreement.get("layby_no"))));
            paymentRequestService.markPaid(paymentRequestId, paid, actor);
            jdbcTemplate.update("UPDATE layby_refund SET status='PAID', paid_at=CURRENT_TIMESTAMP, paid_by=?, payment_reference=?, notes=COALESCE(?,notes) WHERE id=?",
                    actor, paymentReference, clean(request == null ? null : request.getNotes()), refund.get("id"));
            return get(id);
        }

        if (!"APPROVED".equals(upper(refund.get("status")))) throw new IllegalStateException("Refund must be approved before it can be marked paid");
        jdbcTemplate.update("UPDATE layby_refund SET status='PAID', paid_at=CURRENT_TIMESTAMP, paid_by=?, payment_reference=?, notes=COALESCE(?,notes) WHERE id=?",
                actor, paymentReference, clean(request == null ? null : request.getNotes()), refund.get("id"));
        return get(id);
    }

    @Transactional
    public Map<String, Object> fulfil(String id, LaybyDtos.FulfilRequest request, String fallbackActor) {
        Map<String, Object> agreement = lockAgreement(id);
        if (!"PAID_UP".equals(upper(agreement.get("status")))) throw new IllegalStateException("Only a fully paid layby can be fulfilled");
        String actor = actor(firstNonBlank(request == null ? null : request.getActionBy(), fallbackActor));
        StockDtos.SalesOrderIssueRequest issue = new StockDtos.SalesOrderIssueRequest();
        issue.setWarehouseId(text(agreement.get("warehouse_id")));
        issue.setStorageLocationId(null);
        if (request != null) issue.setNotes(clean(request.getNotes()));
        Map<String, Object> order = stockOperationsService.issueSalesOrderForLayby(text(agreement.get("sales_order_id")), issue, actor);
        InvoiceEntity invoice = createFinalInvoice(agreement, order, actor);
        jdbcTemplate.update("UPDATE layby_agreement SET status='FULFILLED', final_invoice_id=?, fulfilled_at=CURRENT_TIMESTAMP, fulfilled_by=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
                invoice.getId(), actor, actor, id);
        history(id, "PAID_UP", "FULFILLED", "Goods released and final invoice " + invoice.getInvoiceNo() + " generated", actor);
        return get(id);
    }

    private InvoiceEntity createFinalInvoice(Map<String, Object> agreement, Map<String, Object> order, String actor) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderLines = (List<Map<String, Object>>) order.get("lines");
        List<InvoiceLineEntity> lines = new ArrayList<>();
        long subtotal = 0L;
        long tax = 0L;
        for (Map<String, Object> line : orderLines) {
            long lineSubtotal = cents(decimal(line.get("line_subtotal")));
            long lineTax = cents(decimal(line.get("line_tax")));
            long lineTotal = cents(decimal(line.get("line_total")));
            subtotal += lineSubtotal;
            tax += lineTax;
            lines.add(InvoiceLineEntity.builder()
                    .productId(text(line.get("product_id")))
                    .description(firstNonBlank(text(line.get("product_description")), "Layby item"))
                    .quantity(decimal(line.get("quantity")).doubleValue())
                    .unitPriceCents(cents(decimal(line.get("unit_price"))))
                    .discountCents(0L).taxCents(lineTax).subtotalCents(lineSubtotal).totalCents(lineTotal)
                    .showAmount(true).build());
        }
        long total = longValue(agreement.get("total_cents"));
        InvoicePaymentEntity settlement = InvoicePaymentEntity.builder()
                .paymentDate(LocalDateTime.now()).amountCents(total).paymentMethod("LAYBY")
                .referenceNo(text(agreement.get("layby_no"))).createdAt(LocalDateTime.now()).createdBy(actor).build();
        InvoiceEntity invoice = InvoiceEntity.builder()
                .externalRef(text(agreement.get("layby_no"))).sourceType("LAYBY").sourceId(text(agreement.get("id")))
                .partnerId(text(agreement.get("customer_partner_id"))).invoiceDate(LocalDate.now()).dueDate(LocalDate.now())
                .status("PAID").subtotalCents(subtotal).taxCents(tax).discountCents(0L).totalCents(total)
                .paidCents(total).creditedCents(0L).balanceCents(0L).currency(firstNonBlank(text(agreement.get("currency")), "ZAR"))
                .notes("Final invoice for layby " + text(agreement.get("layby_no"))).createdBy(actor)
                .lines(lines).payments(new ArrayList<>(List.of(settlement))).build();
        return invoiceService.createInvoice(invoice);
    }

    private void completeCancellation(String id, String actor) {
        Map<String, Object> agreement = lockAgreement(id);
        String current = upper(agreement.get("status"));
        if (!List.of("CANCELLED", "CANCELLATION_PENDING").contains(current)) return;
        Map<String, Object> order = stockOperationsService.getSalesOrder(text(agreement.get("sales_order_id")));
        if (!"ISSUED".equals(upper(order.get("status")))) {
            stockOperationsService.releaseSalesOrderReservation(text(agreement.get("sales_order_id")), actor);
        }
        jdbcTemplate.update("UPDATE layby_agreement SET status='CANCELLED', cancellation_approved_at=CURRENT_TIMESTAMP, cancellation_approved_by=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
                actor, actor, id);
        if (!"CANCELLED".equals(current)) history(id, current, "CANCELLED", "Cancellation approved", actor);
        Map<String, Object> existingRefund = refundOrNull(id);
        if (existingRefund == null) {
            throw new IllegalStateException("Layby refund was not prepared during cancellation request");
        }
        // Use the workflow selected when cancellation was requested. Configuration may be
        // changed later, but an in-flight cancellation must not switch approval mechanisms.
        if (clean(existingRefund.get("payment_request_id")) != null) {
            submitRefundPaymentRequestIfReady(id, actor);
        } else {
            submitManualRefundIfReady(id, actor);
        }
    }

    private void ensureRefund(String laybyId, String refundMethod, String actor) {
        Map<String, Object> agreement = lockAgreement(laybyId);
        long refundAmount = longValue(agreement.get("refund_due_cents"));
        boolean paymentRequestEnabled = refundPaymentRequestEnabled();
        Map<String, Object> existingRefund = refundOrNull(laybyId);
        if (existingRefund != null) {
            if (clean(existingRefund.get("refund_method")) == null) {
                jdbcTemplate.update("UPDATE layby_refund SET refund_method=? WHERE id=?", refundMethod, existingRefund.get("id"));
            }
            if (paymentRequestEnabled && refundAmount > 0 && clean(existingRefund.get("payment_request_id")) == null) {
                PaymentRequestResponse paymentRequest = createCustomerRefundPaymentRequest(agreement, refundMethod, refundAmount, actor);
                jdbcTemplate.update("UPDATE layby_refund SET payment_request_id=?, status='PENDING_SIGNATURE' WHERE id=?",
                        paymentRequest.getId(), existingRefund.get("id"));
            } else if (!paymentRequestEnabled && refundAmount > 0 && clean(existingRefund.get("payment_request_id")) == null
                    && !"PENDING_SIGNATURE".equals(upper(existingRefund.get("status")))) {
                jdbcTemplate.update("UPDATE layby_refund SET status='PENDING_SIGNATURE' WHERE id=?", existingRefund.get("id"));
            }
            return;
        }

        String refundId = uuid();
        String status = refundAmount <= 0 ? "NOT_REQUIRED" : "PENDING_SIGNATURE";
        PaymentRequestResponse paymentRequest = paymentRequestEnabled && refundAmount > 0
                ? createCustomerRefundPaymentRequest(agreement, refundMethod, refundAmount, actor)
                : null;

        jdbcTemplate.update("""
                INSERT INTO layby_refund(id,layby_agreement_id,status,gross_paid_cents,penalty_cents,refund_amount_cents,
                                         cancellation_previous_status,cancellation_reason_code,reason,refund_method,payment_request_id,
                                         requested_at,requested_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?)
                """, refundId, laybyId, status, longValue(agreement.get("paid_cents")), longValue(agreement.get("cancellation_penalty_cents")),
                refundAmount, text(agreement.get("cancellation_previous_status")), text(agreement.get("cancellation_reason_code")),
                text(agreement.get("cancellation_reason")), refundMethod, paymentRequest == null ? null : paymentRequest.getId(), actor);
    }

    private boolean refundPaymentRequestEnabled() {
        return booleanValue(getConfiguration().get("create_refund_payment_request_on_cancellation"));
    }

    private PaymentRequestResponse createCustomerRefundPaymentRequest(
            Map<String, Object> agreement, String refundMethod, long refundAmount, String actor) {
        String laybyId = text(agreement.get("id"));
        String customerId = text(agreement.get("customer_partner_id"));
        String customerName = customerName(customerId);

        PaymentRequestCreateRequest request = new PaymentRequestCreateRequest();
        request.setRequestType(PaymentRequestType.CUSTOMER_REFUND);
        request.setSourceType(PaymentRequestSourceType.LAYBY);
        request.setSourceId(laybyId);
        request.setPayeePartnerId(customerId);
        request.setPayeeName(customerName);
        request.setAmount(BigDecimal.valueOf(refundAmount, 2));
        request.setCurrency(firstNonBlank(text(agreement.get("currency")), "ZAR"));
        request.setPaymentMethod(PaymentMethod.valueOf(refundMethod));
        request.setExternalReference(text(agreement.get("layby_no")));
        request.setPaymentReason("LAYBY-CANCELLATION-REFUND");
        request.setIdempotencyKey("LAYBY:" + laybyId + ":CUSTOMER_REFUND");
        request.setNotes("Customer refund for cancelled layby " + text(agreement.get("layby_no")));
        request.setRequestedPaymentDate(LocalDate.now());

        if (request.getPaymentMethod() == PaymentMethod.EFT) {
            Map<String, Object> banking = activeCustomerBanking(customerId);
            request.setBankName(text(banking.get("bank_name")));
            request.setAccountHolder(text(banking.get("account_holder")));
            request.setAccountNumber(text(banking.get("account_number")));
            request.setBranchCode(text(banking.get("branch_code")));
            request.setAccountType(text(banking.get("account_type")));
        }
        return paymentRequestService.create(request, actor);
    }

    private Map<String, Object> activeCustomerBanking(String customerId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT bank_name, account_holder, account_number, branch_code, account_type
                  FROM partner_bank_account
                 WHERE partner=? AND UPPER(COALESCE(status,''))='ACTIVE'
                   AND (valid_from IS NULL OR valid_from<=CURRENT_DATE)
                   AND (valid_to IS NULL OR valid_to>=CURRENT_DATE)
                 ORDER BY valid_from DESC, id DESC
                 LIMIT 1
                """, customerId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "EFT refund requires approved active banking details for the layby customer.");
        }
        return rows.get(0);
    }

    private String customerName(String customerId) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT TRIM(CONCAT_WS(' ', NULLIF(name2,''), NULLIF(name3,''), NULLIF(name1,'')))
                  FROM partner WHERE id=?
                """, String.class, customerId);
        if (rows.isEmpty() || clean(rows.get(0)) == null) return "Layby Customer";
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> attachSignedCancellationForm(
            String laybyId, LaybyDtos.SignedCancellationFormRequest request, String actorValue) {
        if (request == null || clean(request.getFile()) == null) {
            throw new IllegalArgumentException("Signed cancellation form file is required");
        }
        Map<String, Object> agreement = lockAgreement(laybyId);
        if (!List.of("CANCELLATION_PENDING", "CANCELLED").contains(upper(agreement.get("status")))) {
            throw new IllegalStateException("A cancellation must be requested before the signed cancellation form can be attached");
        }
        Map<String, Object> refund = refund(laybyId);
        String paymentRequestId = clean(refund.get("payment_request_id"));

        byte[] bytes;
        try {
            String file = request.getFile().trim();
            int comma = file.indexOf(',');
            if (file.startsWith("data:") && comma >= 0) file = file.substring(comma + 1);
            bytes = Base64.getDecoder().decode(file);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Signed cancellation form is not valid base64 content", ex);
        }
        if (bytes.length == 0) throw new IllegalArgumentException("Signed cancellation form is empty");

        String extension = firstNonBlank(clean(request.getExtension()), "pdf").replace(".", "").toLowerCase(Locale.ROOT);
        String actor = actor(actorValue);

        // The signed cancellation form is always retained against the Layby for a complete
        // cancellation audit trail. When automatic Payment Request creation is enabled,
        // store the same signed document against the generated Payment Request as well.
        var laybyAttachment = attachmentService.saveBytes(
                bytes, extension, "LAYBY", laybyId, "LAYBY-CANCELLATION-FORM-SIGNED");
        if (paymentRequestId != null) {
            attachmentService.saveBytes(
                    bytes, extension, "PAYMENT-REQUEST", paymentRequestId, "LAYBY-CANCELLATION-FORM-SIGNED");
        }

        String refundStatus = longValue(refund.get("refund_amount_cents")) <= 0
                ? "NOT_REQUIRED" : "READY_FOR_APPROVAL";
        jdbcTemplate.update("UPDATE layby_refund SET signed_cancellation_attachment_id=?, status=? WHERE id=?",
                laybyAttachment.getId(), refundStatus, refund.get("id"));
        if (paymentRequestId == null) {
            submitManualRefundIfReady(laybyId, actor);
        } else {
            submitRefundPaymentRequestIfReady(laybyId, actor);
        }
        return get(laybyId);
    }

    private void submitManualRefundIfReady(String laybyId, String actorValue) {
        Map<String, Object> agreement = lockAgreement(laybyId);
        if (!"CANCELLED".equals(upper(agreement.get("status")))) return;
        Map<String, Object> refund = refund(laybyId);
        if (clean(refund.get("payment_request_id")) != null) return;
        if (longValue(refund.get("refund_amount_cents")) <= 0) {
            jdbcTemplate.update("UPDATE layby_refund SET status='NOT_REQUIRED' WHERE id=?", refund.get("id"));
            return;
        }
        if (clean(refund.get("signed_cancellation_attachment_id")) == null) return;

        String currentStatus = upper(refund.get("status"));
        if (List.of("PENDING_APPROVAL", "APPROVED", "PAID").contains(currentStatus)) return;

        String actor = actor(actorValue);
        if (!booleanValue(getConfiguration().get("require_refund_approval"))) {
            jdbcTemplate.update("UPDATE layby_refund SET status='APPROVED', approved_at=CURRENT_TIMESTAMP, approved_by=? WHERE id=?",
                    actor, refund.get("id"));
            return;
        }

        String approvalReferenceId = uuid();
        jdbcTemplate.update("UPDATE layby_refund SET status='PENDING_APPROVAL', approval_reference_id=?, approval_request_id=NULL, requested_at=CURRENT_TIMESTAMP, requested_by=?, approved_at=NULL, approved_by=NULL WHERE id=?",
                approvalReferenceId, actor, refund.get("id"));
        submitRefundApproval(agreement, text(refund.get("id")), approvalReferenceId,
                longValue(refund.get("refund_amount_cents")), actor);
    }

    private void submitRefundPaymentRequestIfReady(String laybyId, String actorValue) {
        Map<String, Object> agreement = lockAgreement(laybyId);
        if (!"CANCELLED".equals(upper(agreement.get("status")))) return;
        Map<String, Object> refund = refund(laybyId);
        if (longValue(refund.get("refund_amount_cents")) <= 0) return;
        if (clean(refund.get("signed_cancellation_attachment_id")) == null) return;
        String paymentRequestId = clean(refund.get("payment_request_id"));
        if (paymentRequestId == null) return;

        PaymentRequestResponse paymentRequest = paymentRequestService.getById(paymentRequestId);
        if (paymentRequest.getStatus() != PaymentRequestStatus.DRAFT) {
            syncRefundFromPaymentRequest(refund, paymentRequest);
            return;
        }

        String actor = actor(actorValue);
        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.PAYMENT_REQUEST);
        approval.setReferenceId(paymentRequestId);
        approval.setReferenceNo(paymentRequest.getRequestNo());
        approval.setTitle("Layby refund " + text(agreement.get("layby_no")) + " - " + paymentRequest.getPayeeName());
        approval.setDescription("Customer refund for cancelled layby " + text(agreement.get("layby_no"))
                + ". Signed cancellation form is attached to the payment request.");
        approval.setRequesterId(actor);
        approval.setPayloadJson(toJson(paymentRequest));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        jdbcTemplate.update("UPDATE layby_refund SET status='PENDING_APPROVAL', payment_request_approval_id=? WHERE id=?",
                response.getId(), refund.get("id"));
    }

    private void syncRefundFromPaymentRequest(Map<String, Object> refund, PaymentRequestResponse paymentRequest) {
        String status = switch (paymentRequest.getStatus()) {
            case APPROVED, QUEUED_FOR_PAYMENT, PROCESSED -> "APPROVED";
            case PAID -> "PAID";
            case REJECTED -> "REJECTED";
            case CANCELLED -> "CANCELLED";
            case PENDING_APPROVAL -> "PENDING_APPROVAL";
            default -> clean(refund.get("signed_cancellation_attachment_id")) == null ? "PENDING_SIGNATURE" : "READY_FOR_APPROVAL";
        };
        jdbcTemplate.update("UPDATE layby_refund SET status=? WHERE id=?", status, refund.get("id"));
    }

    private void cancelDraftRefundPaymentRequest(String laybyId, String actor) {
        Map<String, Object> refund = refundOrNull(laybyId);
        if (refund == null) return;
        String paymentRequestId = clean(refund.get("payment_request_id"));
        if (paymentRequestId != null) {
            PaymentRequestResponse paymentRequest = paymentRequestService.getById(paymentRequestId);
            if (paymentRequest.getStatus() == PaymentRequestStatus.DRAFT
                    || paymentRequest.getStatus() == PaymentRequestStatus.PENDING_APPROVAL) {
                paymentRequestService.cancel(paymentRequestId, "Layby cancellation rejected", actor);
            }
        }
        jdbcTemplate.update("UPDATE layby_refund SET status='CANCELLED', notes=? WHERE id=?",
                "Layby cancellation rejected", refund.get("id"));
    }

    private Map<String, Object> refundOrNull(String laybyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM layby_refund WHERE layby_agreement_id=?", laybyId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String normalizeRefundMethod(String value) {
        String method = upper(value);
        if (!List.of("CASH", "EFT").contains(method)) {
            throw new IllegalArgumentException("Refund method must be CASH or EFT");
        }
        return method;
    }

    private void submitRefundApproval(Map<String, Object> agreement, String refundId, String approvalReferenceId, long refundAmount, String actor) {
        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.LAYBY_REFUND);
        approval.setReferenceId(approvalReferenceId);
        approval.setReferenceNo(text(agreement.get("layby_no")));
        approval.setTitle("Refund cancelled layby " + text(agreement.get("layby_no")));
        approval.setDescription("Review the customer refund after layby cancellation.");
        approval.setRequesterId(actor);
        approval.setPayloadJson(toJson(Map.of(
                "laybyId", text(agreement.get("id")),
                "laybyNo", text(agreement.get("layby_no")),
                "customerPartnerId", text(agreement.get("customer_partner_id")),
                "grossPaidCents", longValue(agreement.get("paid_cents")),
                "penaltyCents", longValue(agreement.get("cancellation_penalty_cents")),
                "refundAmountCents", refundAmount)));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        jdbcTemplate.update("UPDATE layby_refund SET approval_request_id=? WHERE id=?", response.getId(), refundId);
    }

    private Map<String, Object> refund(String laybyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM layby_refund WHERE layby_agreement_id=?", laybyId);
        if (rows.isEmpty()) throw new IllegalStateException("No refund exists for this layby");
        return rows.get(0);
    }

    private void createInstallments(String laybyId, long totalCents, int count, LocalDate firstDue, String frequency, String actor) {
        long base = totalCents / count;
        long remainder = totalCents % count;
        for (int i = 0; i < count; i++) {
            long amount = base + (i < remainder ? 1 : 0);
            jdbcTemplate.update("""
                    INSERT INTO layby_installment(id,layby_agreement_id,installment_no,due_date,amount_cents,paid_cents,balance_cents,status,created_at,created_by)
                    VALUES(?,?,?,?,?,0,?,'PENDING',CURRENT_TIMESTAMP,?)
                    """, uuid(), laybyId, i + 1, Date.valueOf(dueDate(firstDue, frequency, i)), amount, amount, actor);
        }
    }

    private void allocateToInstallments(String laybyId, String receiptId, long amountCents, String actor) {
        long remaining = amountCents;
        List<Map<String, Object>> installments = jdbcTemplate.queryForList("SELECT * FROM layby_installment WHERE layby_agreement_id=? AND balance_cents>0 ORDER BY due_date, installment_no FOR UPDATE", laybyId);
        for (Map<String, Object> installment : installments) {
            if (remaining <= 0) break;
            long balance = longValue(installment.get("balance_cents"));
            long allocated = Math.min(balance, remaining);
            long newPaid = longValue(installment.get("paid_cents")) + allocated;
            long newBalance = balance - allocated;
            String status = newBalance == 0 ? "PAID" : "PARTIAL";
            jdbcTemplate.update("UPDATE layby_installment SET paid_cents=?, balance_cents=?, status=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
                    newPaid, newBalance, status, actor, installment.get("id"));
            jdbcTemplate.update("INSERT INTO layby_installment_payment(id,layby_agreement_id,layby_installment_id,receipt_id,amount_cents,created_at,created_by) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?)",
                    uuid(), laybyId, installment.get("id"), receiptId, allocated, actor);
            remaining -= allocated;
        }
        if (remaining != 0) throw new IllegalStateException("Unable to allocate the full layby payment");
    }

    private void refreshArrearsStatuses() {
        jdbcTemplate.update("""
                UPDATE layby_agreement l
                   SET l.status='IN_ARREARS', l.updated_at=CURRENT_TIMESTAMP
                 WHERE l.status='ACTIVE'
                   AND EXISTS (SELECT 1 FROM layby_installment i WHERE i.layby_agreement_id=l.id AND i.due_date<CURRENT_DATE AND i.balance_cents>0)
                """);
        jdbcTemplate.update("""
                UPDATE layby_agreement l
                   SET l.status='ACTIVE', l.updated_at=CURRENT_TIMESTAMP
                 WHERE l.status='IN_ARREARS'
                   AND NOT EXISTS (SELECT 1 FROM layby_installment i WHERE i.layby_agreement_id=l.id AND i.due_date<CURRENT_DATE AND i.balance_cents>0)
                """);
    }

    private void refreshArrearsStatus(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT status FROM layby_agreement WHERE id=?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Layby not found: " + id);
        String status = upper(rows.get(0).get("status"));
        if (!List.of("ACTIVE", "IN_ARREARS").contains(status)) return;
        String computed = paymentStatus(id);
        if (!status.equals(computed)) jdbcTemplate.update("UPDATE layby_agreement SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?", computed, id);
    }

    private String paymentStatus(String id) {
        Integer overdue = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM layby_installment WHERE layby_agreement_id=? AND due_date<CURRENT_DATE AND balance_cents>0", Integer.class, id);
        return overdue != null && overdue > 0 ? "IN_ARREARS" : "ACTIVE";
    }

    private Map<String, Object> lockAgreement(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM layby_agreement WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Layby not found: " + id);
        return rows.get(0);
    }

    private void setStatus(String id, String status, String reason, String actor) {
        Map<String, Object> agreement = lockAgreement(id);
        String previous = upper(agreement.get("status"));
        jdbcTemplate.update("UPDATE layby_agreement SET status=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?", status, actor, id);
        history(id, previous, status, reason, actor);
    }

    private void history(String id, String previous, String next, String reason, String actor) {
        jdbcTemplate.update("INSERT INTO layby_status_history(id,layby_agreement_id,previous_status,new_status,reason,changed_at,changed_by) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?)",
                uuid(), id, previous, next, reason, actor);
    }

    private void addOperationalFlags(Map<String, Object> row, BusinessCalendar businessCalendar) {
        String status = upper(row.get("status"));
        long balance = longValue(row.get("balance_cents"));
        LocalDate completion = localDate(row.get("expected_completion_date"));
        int graceDays = integer(row.get("grace_business_days"));
        LocalDate eligibleDate = completion == null ? null : addBusinessDays(completion, graceDays, businessCalendar);
        row.put("default_eligible_date", eligibleDate);
        row.put("default_eligible", eligibleDate != null
                && balance > 0
                && List.of("ACTIVE", "IN_ARREARS").contains(status)
                && !LocalDate.now().isBefore(eligibleDate));
    }

    private LocalDate addBusinessDays(LocalDate start, int businessDays, BusinessCalendar businessCalendar) {
        LocalDate date = start;
        int remaining = Math.max(0, businessDays);
        while (remaining > 0) {
            date = date.plusDays(1);
            if (isLaybyBusinessDay(date, businessCalendar)) remaining--;
        }
        return date;
    }

    private BusinessCalendar loadBusinessCalendar() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, monday_working, tuesday_working, wednesday_working, thursday_working,
                       friday_working, saturday_working, sunday_working
                  FROM working_calendar
                 WHERE active=1
                 ORDER BY CASE WHEN UPPER(code)='STANDARD' THEN 0 ELSE 1 END, code
                 LIMIT 1
                """);
        Map<String, Object> calendar = rows.isEmpty()
                ? Map.of("monday_working", true, "tuesday_working", true, "wednesday_working", true,
                         "thursday_working", true, "friday_working", true,
                         "saturday_working", false, "sunday_working", false)
                : rows.get(0);
        String calendarId = text(calendar.get("id"));
        List<Map<String, Object>> holidays = calendarId == null || calendarId.isBlank()
                ? List.of()
                : jdbcTemplate.queryForList("""
                        SELECT holiday_date, recurring_annual
                          FROM working_calendar_holiday
                         WHERE working_calendar_id=? AND active=1
                        """, calendarId);
        return new BusinessCalendar(calendar, holidays);
    }

    private boolean isLaybyBusinessDay(LocalDate date, BusinessCalendar businessCalendar) {
        Map<String, Object> calendar = businessCalendar.calendar();
        String key = switch (date.getDayOfWeek()) {
            case MONDAY -> "monday_working";
            case TUESDAY -> "tuesday_working";
            case WEDNESDAY -> "wednesday_working";
            case THURSDAY -> "thursday_working";
            case FRIDAY -> "friday_working";
            case SATURDAY -> "saturday_working";
            case SUNDAY -> "sunday_working";
        };
        if (!booleanValue(calendar.get(key))) return false;
        return businessCalendar.holidays().stream().noneMatch(holiday -> {
            LocalDate holidayDate = localDate(holiday.get("holiday_date"));
            if (holidayDate == null) return false;
            if (booleanValue(holiday.get("recurring_annual"))) {
                return holidayDate.getMonthValue() == date.getMonthValue() && holidayDate.getDayOfMonth() == date.getDayOfMonth();
            }
            return holidayDate.equals(date);
        });
    }

    private record BusinessCalendar(Map<String, Object> calendar, List<Map<String, Object>> holidays) { }

    private LocalDate localDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        String raw = value.toString().trim();
        if (raw.isEmpty()) return null;
        return LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw);
    }

    private void requireWarehouseForStockControlledLines(String salesOrderId, String warehouseId) {
        if (clean(warehouseId) != null) return;
        Integer stockControlled = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM sales_order_line sol
                  JOIN product p ON p.id=sol.product_id
                 WHERE sol.sales_order_id=?
                   AND UPPER(REPLACE(COALESCE(NULLIF(TRIM(p.type),''),'PHYSICAL-PRODUCT'),'_','-'))
                       IN ('PHYSICAL-PRODUCT','CONSUMABLE','TOMBSTONE')
                """, Integer.class, salesOrderId);
        if (stockControlled != null && stockControlled > 0) {
            throw new IllegalArgumentException("A warehouse is required when a layby contains stock-controlled products");
        }
    }

    private void requireCustomer(String customerId) {
        if (clean(customerId) == null) throw new IllegalArgumentException("customerPartnerId is required");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM partner p JOIN partner_role pr ON pr.partner=p.id AND pr.role='CUSTOMER' WHERE p.id=?", Integer.class, customerId);
        if (count == null || count == 0) throw new IllegalArgumentException("Customer not found or partner does not have CUSTOMER role: " + customerId);
    }

    private void ensureConfiguration() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM layby_configuration WHERE id=1", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO layby_configuration(id,enabled,default_payment_frequency,default_duration_months,deposit_required,
                        minimum_deposit_percent,cancellation_penalty_percent,default_grace_business_days,require_cancellation_approval,
                        require_refund_approval,create_refund_payment_request_on_cancellation,automatically_reserve_stock,allow_stock_short_layby)
                    VALUES(1,1,'MONTHLY',3,0,0,1,60,1,1,1,1,0)
                    """);
        }
    }

    private String normalizeFrequency(String value) {
        String frequency = firstNonBlank(value, "MONTHLY").trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_FREQUENCIES.contains(frequency)) throw new IllegalArgumentException("Unsupported payment frequency: " + frequency);
        return frequency;
    }

    private int defaultInstallmentCount(String frequency, int months) {
        return switch (frequency) {
            case "WEEKLY" -> Math.max(1, months * 4);
            case "FORTNIGHTLY" -> Math.max(1, months * 2);
            case "ONCE" -> 1;
            default -> Math.max(1, months);
        };
    }

    private LocalDate firstDueDate(String frequency, boolean depositRequired) {
        if (depositRequired) return LocalDate.now();
        return dueDate(LocalDate.now(), frequency, 1);
    }

    private LocalDate dueDate(LocalDate first, String frequency, int offset) {
        return switch (frequency) {
            case "WEEKLY" -> first.plusWeeks(offset);
            case "FORTNIGHTLY" -> first.plusWeeks((long) offset * 2L);
            case "MONTHLY" -> first.plusMonths(offset);
            case "ONCE" -> first;
            default -> throw new IllegalArgumentException("Unsupported payment frequency: " + frequency);
        };
    }

    private void requirePercent(BigDecimal value, int min, int max, String field) {
        if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
    }

    private long percentOf(long cents, BigDecimal percent) {
        return BigDecimal.valueOf(cents).multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
    }

    private long cents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        String text = value.toString().trim();
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        String text = value.toString().trim();
        return text.isEmpty() ? 0L : new BigDecimal(text).longValue();
    }

    private int integer(Object value) {
        return (int) longValue(value);
    }

    private boolean booleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return "true".equalsIgnoreCase(value.toString()) || "1".equals(value.toString());
    }

    private int bool(Boolean requested, Object fallback) {
        return requested == null ? (booleanValue(fallback) ? 1 : 0) : (requested ? 1 : 0);
    }

    private String actor(String actor) {
        return clean(actor) == null ? "SYSTEM" : actor.trim();
    }

    private String clean(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (clean(value) != null) return value.trim();
        return null;
    }

    private String text(Object value) {
        return value == null ? "" : Objects.toString(value, "");
    }

    private String upper(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to prepare layby approval payload", e);
        }
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }
}
