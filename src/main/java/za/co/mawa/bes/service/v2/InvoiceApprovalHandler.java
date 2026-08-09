package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.InvoiceRepository;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class InvoiceApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {

    private final InvoiceRepository invoiceRepository;

    @Override
    public ApprovalType supports() {
        return ApprovalType.INVOICE;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        InvoiceEntity invoice = getInvoice(approvalRequest);
        String status = normalized(invoice.getStatus());
        if (!("DRAFT".equals(status) || "NEW".equals(status) || "REJECTED".equals(status))) {
            throw new IllegalStateException("Only draft invoices can be submitted for approval. Current status: " + status);
        }
        invoice.setStatus("AWAITING_APPROVAL");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actionBy);
        invoiceRepository.save(invoice);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        InvoiceEntity invoice = getInvoice(approvalRequest);
        long balance = value(invoice.getBalanceCents());
        long paid = value(invoice.getPaidCents());
        if (balance <= 0) {
            invoice.setStatus("PAID");
        } else if (paid > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        } else {
            invoice.setStatus("ISSUED");
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actionBy);
        invoiceRepository.save(invoice);
    }

    @Override
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        InvoiceEntity invoice = getInvoice(approvalRequest);
        invoice.setStatus("REJECTED");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actionBy);
        invoiceRepository.save(invoice);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        InvoiceEntity invoice = getInvoice(approvalRequest);
        invoice.setStatus("DRAFT");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actionBy);
        invoiceRepository.save(invoice);
    }

    private InvoiceEntity getInvoice(ApprovalRequestEntity approvalRequest) {
        return invoiceRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invoice not found: " + approvalRequest.getReferenceId()));
    }

    private long value(Long amount) {
        return amount == null ? 0L : amount;
    }

    private String normalized(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
