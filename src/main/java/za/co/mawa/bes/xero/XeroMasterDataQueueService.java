package za.co.mawa.bes.xero;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.repository.PartnerRoleRepository;
import za.co.mawa.bes.repository.InvoiceRepository;

import java.time.LocalDateTime;

@Service
public class XeroMasterDataQueueService {
    public static final String CUSTOMER_MESSAGE_TYPE = "XERO-CUSTOMER";
    public static final String PRODUCT_MESSAGE_TYPE = "XERO-PRODUCT";

    private final MessageQueueRepository queueRepository;
    private final XeroIntegrationSettingsService settings;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final PartnerRoleRepository partnerRoleRepository;
    private final InvoiceRepository invoiceRepository;
    private final XeroInvoiceQueueService invoiceQueueService;

    public XeroMasterDataQueueService(MessageQueueRepository queueRepository,
                                      XeroIntegrationSettingsService settings,
                                      PartnerRepository partnerRepository,
                                      ProductRepository productRepository,
                                      PartnerRoleRepository partnerRoleRepository,
                                      InvoiceRepository invoiceRepository,
                                      XeroInvoiceQueueService invoiceQueueService) {
        this.queueRepository = queueRepository;
        this.settings = settings;
        this.partnerRepository = partnerRepository;
        this.productRepository = productRepository;
        this.partnerRoleRepository = partnerRoleRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceQueueService = invoiceQueueService;
    }

    public void queueCustomerIfEnabled(String partnerId, String partnerNo) {
        if (partnerId == null || partnerId.isBlank()) return;
        boolean customer = partnerRoleRepository.findRoleByPartner(partnerId).stream()
                .anyMatch(role -> role.getPartnerRolePK() != null
                        && "CUSTOMER".equalsIgnoreCase(role.getPartnerRolePK().getRole()));
        if (!customer) return;
        queueIfEnabled(CUSTOMER_MESSAGE_TYPE, partnerId, partnerNo);
    }

    public void queueProductIfEnabled(String productId, String productCode) {
        queueIfEnabled(PRODUCT_MESSAGE_TYPE, productId, productCode);
    }

    public void queueAllExisting() {
        if (!settings.isIntegrationEnabled()) return;

        // Products are deliberately queued first. The shared message worker consumes
        // a bounded batch ordered by next-attempt time; queuing every customer first
        // can otherwise leave the complete product catalogue waiting behind a large
        // customer backlog.
        productRepository.findAll().forEach(product ->
                queueProductIfEnabled(product.getId(), product.getCode()));

        partnerRoleRepository.findPartnerByRole("CUSTOMER").forEach(role ->
                partnerRepository.findById(role.getPartnerRolePK().getId()).ifPresent(partner ->
                        queueCustomerIfEnabled(partner.getId(), partner.getNo())));

        // Activation previously omitted existing invoices completely. Keep invoices
        // last because their push validates/synchronises the referenced customer and
        // products before submitting the invoice to Xero.
        invoiceRepository.findAll().forEach(invoiceQueueService::queueInvoiceIfEnabled);
    }

    private void queueIfEnabled(String type, String id, String number) {
        if (!settings.isIntegrationEnabled() || id == null || id.isBlank()) return;
        MessageQueueEntity message = queueRepository
                .findFirstByTypeAndReferenceIdOrderByIdDesc(type, id)
                .orElseGet(MessageQueueEntity::new);
        message.setType(type);
        message.setReferenceId(id);
        message.setReferenceNo(number);
        message.setPayload(id);
        message.setProcessed(false);
        message.setRetryCount(0);
        message.setNextAttemptAt(LocalDateTime.now());
        queueRepository.save(message);
    }
}
