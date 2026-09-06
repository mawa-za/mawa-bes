package za.co.mawa.bes.xero;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.repository.PartnerRoleRepository;

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

    public XeroMasterDataQueueService(MessageQueueRepository queueRepository,
                                      XeroIntegrationSettingsService settings,
                                      PartnerRepository partnerRepository,
                                      ProductRepository productRepository,
                                      PartnerRoleRepository partnerRoleRepository) {
        this.queueRepository = queueRepository;
        this.settings = settings;
        this.partnerRepository = partnerRepository;
        this.productRepository = productRepository;
        this.partnerRoleRepository = partnerRoleRepository;
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
        partnerRoleRepository.findPartnerByRole("CUSTOMER").forEach(role ->
                partnerRepository.findById(role.getPartnerRolePK().getId()).ifPresent(partner ->
                        queueCustomerIfEnabled(partner.getId(), partner.getNo())));
        productRepository.findAll().forEach(product -> queueProductIfEnabled(product.getId(), product.getCode()));
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
