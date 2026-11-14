package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.File;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/** Current invoice PDF sent through the shared outgoing-mail gateway. */
@Service
public class InvoiceOutboundEmailService {
    private final InvoiceService invoices;
    private final SharedOutgoingEmailService outgoing;
    private final UserAccessService access;
    private final boolean enabled;
    private final boolean queueEnabled;
    private final QueuedOutgoingEmailService queue;

    @Autowired
    public InvoiceOutboundEmailService(InvoiceService invoices,
                                       SharedOutgoingEmailService outgoing,
                                       UserAccessService access,
                                       QueuedOutgoingEmailService queue,
                                       @Value("${mawa.outgoing.invoice.enabled:false}") boolean enabled,
                                       @Value("${mawa.outgoing.queue.enabled:false}") boolean queueEnabled) {
        this.invoices = invoices;
        this.outgoing = outgoing;
        this.access = access;
        this.queue = queue;
        this.enabled = enabled;
        this.queueEnabled = queueEnabled;
    }

    /** Preserve existing Phase 1 constructor for pre-queue tests. */
    InvoiceOutboundEmailService(InvoiceService invoices, SharedOutgoingEmailService outgoing,
                                UserAccessService access, boolean enabled) {
        this(invoices, outgoing, access, null, enabled, false);
    }

    public Result submit(String invoiceId, String recipient) {
        if (!enabled) throw new IllegalStateException("Invoice outgoing email is not enabled");
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())
                || !StringUtils.hasText(UserContext.getCurrentUserId())
                || UserContext.isBackgroundSession() || UserContext.isPlatformSession()
                || !access.hasWorkcentreAccess("invoice", null)) {
            throw new SecurityException("Invoice emailing requires an authorised tenant user");
        }
        access.assertExternalTransactionAllowed("send invoice email");
        String destination = SharedOutgoingEmailService.validatedSingleAddress(recipient);
        InvoiceEntity invoice = invoices.getInvoice(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        String status = invoice.getStatus() == null ? "" : invoice.getStatus().trim().toUpperCase(Locale.ROOT);
        if (status.isEmpty() || List.of("DRAFT", "CANCELLED", "VOIDED").contains(status)) {
            throw new IllegalArgumentException("Only issued invoices can be emailed");
        }
        if (!StringUtils.hasText(invoice.getPartnerId())) {
            throw new IllegalArgumentException("Invoice customer is not configured");
        }
        String number = StringUtils.hasText(invoice.getInvoiceNo()) ? invoice.getInvoiceNo() : invoice.getId();
        number = number.replaceAll("[\\r\\n]", " ");
        String escaped = HtmlUtils.htmlEscape(number);
        String html = "<html><body><p>Please find invoice <strong>" + escaped
                + "</strong> attached.</p><p>Regards</p></body></html>";
        File pdf = new File();
        pdf.setName("invoice_" + number.replaceAll("[^A-Za-z0-9_-]", "_"));
        pdf.setType("pdf");
        pdf.setContent(Base64.getEncoder().encodeToString(invoices.generateInvoicePdf(invoice.getId()).toByteArray()));
        if (queueEnabled) {
            String requestId = queue.queue(destination, "Invoice " + number,
                    html, List.of(pdf), "invoice", invoice.getId());
            return new Result("QUEUED", requestId,
                    "Invoice email queued for sending; delivery is not yet confirmed");
        }
        String requestId = outgoing.submit(destination, "Invoice " + number,
                html, List.of(pdf), "invoice");
        return new Result("ACCEPTED", requestId,
                "Invoice email accepted by the outgoing mail transport; delivery is not yet confirmed");
    }

    /** Read status only for invoices the logged-in tenant user can access. */
    public TenantOutgoingEmailOutboxStore.EmailStatus status(String invoiceId, String requestId) {
        if (!enabled || !queueEnabled) throw new IllegalStateException("Invoice email queue is unavailable");
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())
                || !StringUtils.hasText(UserContext.getCurrentUserId())
                || UserContext.isBackgroundSession() || UserContext.isPlatformSession()
                || !access.hasWorkcentreAccess("invoice", null)) {
            throw new SecurityException("Invoice email status requires an authorised tenant user");
        }
        if (invoices.getInvoice(invoiceId).isEmpty()) {
            throw new NoSuchElementException("Invoice not found");
        }
        return queue.invoiceStatus(invoiceId, requestId)
                .orElseThrow(() -> new NoSuchElementException("Invoice email request not found"));
    }

    public record Result(String status, String requestId, String message) {}
}
