package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.invoice.CaptureInvoicePaymentDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.SyncStatus;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InvoicePaymentService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;
    private final OnlineCashupService onlineCashupService;
    private final NumberAllocationService numberAllocationService;

    @Transactional
    public PaymentBatchResponseDto capturePayment(String invoiceId, CaptureInvoicePaymentDto request) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("invoiceId is required");
        }
        if (request == null || request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required");
        }

        InvoiceEntity invoice = invoiceRepository.findByIdForUpdate(invoiceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        String currentStatus = normalize(invoice.getStatus());
        if (!("ISSUED".equals(currentStatus)
                || "PARTIALLY_PAID".equals(currentStatus)
                || "OVERDUE".equals(currentStatus))) {
            throw new IllegalStateException(
                    "Payment can only be captured for an approved/issued invoice. Current status: " + currentStatus);
        }

        long balance = value(invoice.getBalanceCents());
        if (balance <= 0) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNo() + " has no outstanding balance");
        }
        if (request.getAmountCents() > balance) {
            throw new IllegalArgumentException("Payment amount exceeds invoice balance");
        }

        String actor = blank(request.getCreatedBy()) ? "SYSTEM" : request.getCreatedBy().trim();
        LocalDate paymentDate = request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate();
        LocalDateTime paymentDateTime = paymentDate.atStartOfDay();
        String paymentMethod = request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        String deviceId = blank(request.getDeviceId()) ? "ERP-ONLINE" : request.getDeviceId().trim();

        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(numberAllocationService.allocateNumber("PAYMENT_BATCH"));
        batch.setSourceType(ReceiptSourceType.INVOICE);
        batch.setReceivedFromPartnerId(invoice.getPartnerId());
        batch.setPaymentMethod(paymentMethod);
        batch.setTotalAmountCents(request.getAmountCents());
        batch.setPaymentDate(paymentDateTime);
        batch.setLocation(request.getLocation());
        batch.setEmployeeResponsible(request.getEmployeeResponsible());
        batch.setDeviceId(deviceId);
        batch.setTerminalId(request.getTerminalId());
        batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setNotes(request.getNotes());
        batch.setCreatedAt(LocalDateTime.now());
        batch.setCreatedBy(actor);
        batch = paymentBatchRepository.save(batch);

        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(numberAllocationService.allocateNumber("RECEIPT"));
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.INVOICE);
        receipt.setReceivedFromPartnerId(invoice.getPartnerId());
        receipt.setReceiptDate(paymentDateTime);
        receipt.setPaymentMethod(paymentMethod);
        receipt.setTotalAmountCents(request.getAmountCents());
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setSyncStatus(SyncStatus.SYNCED);
        receipt.setLocation(request.getLocation());
        receipt.setEmployeeResponsible(request.getEmployeeResponsible());
        receipt.setDeviceId(deviceId);
        receipt.setTerminalId(request.getTerminalId());
        receipt.setCaptureSource("ERP_ONLINE");
        receipt.setCapturedBy(actor);
        receipt.setPrinted(false);
        receipt.setPrintCount(0);
        receipt.setNotes(invoiceReceiptNotes(invoice, request));
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(actor);
        receipt = receiptRepository.save(receipt);

        InvoicePaymentEntity payment = InvoicePaymentEntity.builder()
                .invoice(invoice)
                .paymentDate(paymentDateTime)
                .amountCents(request.getAmountCents())
                .paymentMethod(paymentMethod)
                .referenceNo(blank(request.getReference()) ? receipt.getReceiptNo() : request.getReference().trim())
                .createdAt(LocalDateTime.now())
                .createdBy(actor)
                .build();
        invoicePaymentRepository.save(payment);

        var allocation = receiptService.createAllocation(
                receipt.getId(),
                ReceiptAllocationType.INVOICE,
                invoice.getId(),
                invoice.getInvoiceNo(),
                null,
                null,
                request.getAmountCents(),
                actor
        );

        long newPaid = value(invoice.getPaidCents()) + request.getAmountCents();
        long newBalance = Math.max(0L,
                value(invoice.getTotalCents()) - newPaid - value(invoice.getCreditedCents()));
        invoice.setPaidCents(newPaid);
        invoice.setBalanceCents(newBalance);
        invoice.setStatus(newBalance == 0 ? "PAID" : "PARTIALLY_PAID");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actor);
        invoiceRepository.save(invoice);

        onlineCashupService.addReceipts(batch, List.of(receipt.getId()), actor, deviceId);
        ReceiptResponseDto receiptDto = receiptMapper.toDto(receipt, List.of(allocation));

        return PaymentBatchResponseDto.builder()
                .id(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .sourceType(batch.getSourceType())
                .receivedFromPartnerId(batch.getReceivedFromPartnerId())
                .paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents())
                .paymentDate(batch.getPaymentDate())
                .location(batch.getLocation())
                .employeeResponsible(batch.getEmployeeResponsible())
                .deviceId(batch.getDeviceId())
                .terminalId(batch.getTerminalId())
                .status(batch.getStatus())
                .syncStatus(batch.getSyncStatus())
                .notes(batch.getNotes())
                .createdAt(batch.getCreatedAt())
                .createdBy(batch.getCreatedBy())
                .receipts(List.of(receiptDto))
                .build();
    }

    private String invoiceReceiptNotes(InvoiceEntity invoice, CaptureInvoicePaymentDto request) {
        String base = "Payment for invoice " + invoice.getInvoiceNo();
        if (blank(request.getNotes())) return base;
        return base + " - " + request.getNotes().trim();
    }

    private long value(Long amount) {
        return amount == null ? 0L : amount;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
