package za.co.mawa.bes.xero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.service.UserAccessService;

import java.time.LocalDateTime;

@Service
public class XeroInvoiceQueueService {

    public static final String MESSAGE_TYPE = "XERO-INVOICE";

    @Autowired
    private MessageQueueRepository messageQueueRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private XeroIntegrationSettingsService xeroIntegrationSettingsService;

    @Autowired
    private UserAccessService userAccessService;

    public void queueInvoiceIfEnabled(InvoiceEntity invoice) {
        if (invoice == null || isBlank(invoice.getId())) {
            return;
        }
        if (!xeroIntegrationSettingsService.isInvoiceIntegrationEnabled()) {
            invoice.setIntegrationStatus("NOT_ENABLED");
            invoiceRepository.save(invoice);
            return;
        }
        if (userAccessService.externalTransactionsBlockedForInteractiveSession()) {
            invoice.setIntegrationStatus("BLOCKED_TEST_USER");
            invoice.setIntegrationError("Xero submission blocked by testing-user access policy");
            invoiceRepository.save(invoice);
            userAccessService.audit("EXTERNAL_TRANSACTION_BLOCKED", "INVOICE", invoice.getId(),
                    "Test-user access policy", "XERO-INVOICE");
            return;
        }
        queueInvoice(invoice);
    }

    public void queueInvoice(InvoiceEntity invoice) {
        if (invoice == null || isBlank(invoice.getId())) {
            return;
        }
        if (!isBlank(invoice.getXeroInvoiceId())) {
            invoice.setIntegrationStatus("POSTED");
            invoiceRepository.save(invoice);
            return;
        }

        MessageQueueEntity message = messageQueueRepository
                .findFirstByTypeAndReferenceIdOrderByIdDesc(MESSAGE_TYPE, invoice.getId())
                .orElseGet(MessageQueueEntity::new);

        message.setType(MESSAGE_TYPE);
        message.setReferenceId(invoice.getId());
        message.setReferenceNo(invoice.getInvoiceNo());
        message.setPayload(invoice.getId());
        message.setProcessed(false);
        message.setRetryCount(0);
        message.setNextAttemptAt(LocalDateTime.now());
        messageQueueRepository.save(message);

        invoice.setIntegrationStatus("QUEUED");
        invoice.setIntegrationError(null);
        invoiceRepository.save(invoice);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
