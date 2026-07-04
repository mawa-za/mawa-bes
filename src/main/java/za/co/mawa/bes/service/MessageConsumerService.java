package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.fnb.BankPaymentService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.PaymentInformation;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.xero.XeroInvoicePushService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageConsumerService {

    @Autowired
    UserService userService;
    @Autowired
    MessageQueueRepository messageQueueRepository;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    BankPaymentService bankPaymentService;
    @Autowired
    @Qualifier("paymentRequestServiceV2")
    za.co.mawa.bes.service.v2.PaymentRequestService paymentRequestService;
    @Autowired
    XeroInvoicePushService xeroInvoicePushService;
    Gson gson = new Gson();

    @Scheduled(fixedDelay = 60000)
    public void processAllTenants() {
        ObjectMapper mapper = new ObjectMapper();
        for (TenantDto tenant : tenantAdminService.getAll()) {

            try {
                TenantContext.setCurrentTenant(tenant.getId());
                List<MessageQueueEntity> messageQueueEntities = messageQueueRepository
                        .findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime.now());

                for (MessageQueueEntity msg : messageQueueEntities) {
                    try {
                        System.out.println("Tenant: " + tenant + " Payload: " + msg.getPayload());
                        switch (msg.getType()) {
                            case "FNB-EFT-PAYMENT":
                                String instructionId = bankPaymentService.sendPaymentRequest(msg.getPayload());
                                BankPaymentRequest bankPaymentRequest = mapper.readValue(msg.getPayload(), BankPaymentRequest.class);
                                String systemUserId = userService.getUserByName("BGUSER").getId();

                                if (msg.getReferenceId() != null && !msg.getReferenceId().isBlank()) {
                                    paymentRequestService.markSentToBank(msg.getReferenceId(), instructionId, systemUserId);
                                } else {
                                    for (PaymentInformation paymentInformation : bankPaymentRequest.getPaymentInformation()) {
                                        paymentRequestService.markSentToBank(
                                                paymentInformation.getPaymentInformationId(),
                                                instructionId,
                                                systemUserId
                                        );
                                    }
                                }

                                msg.setProcessed(true);
                                break;
                            case "INVOICE-EMAIL":
//                                paymentRequestService.sendInvoiceFile(msg.getPayload());
                                msg.setProcessed(true);
                                break;
                            case "XERO-INVOICE":
                                xeroInvoicePushService.pushInvoice(resolveInvoiceId(msg));
                                msg.setProcessed(true);
                                break;
                            default:
                                // code block
                        }

                    } catch (Exception e) {
                        if ("XERO-INVOICE".equals(msg.getType())) {
                            xeroInvoicePushService.markFailed(resolveInvoiceId(msg), e.getMessage());
                        }
                        msg.setRetryCount(msg.getRetryCount() + 1);
                        if (msg.getRetryCount() > 3) {
                            msg.setProcessed(true); // Optionally move to DeadLetterQueue
                        } else {
                            msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(10));
                        }
                    }
                    messageQueueRepository.save(msg);
                }

            } catch (Exception e) {
                System.err.println("Error processing tenant " + tenant + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private String resolveInvoiceId(MessageQueueEntity msg) {
        if (msg.getReferenceId() != null && !msg.getReferenceId().isBlank()) {
            return msg.getReferenceId();
        }
        return msg.getPayload();
    }

    private void sendInvoice(MessageQueueEntity msg) {
        try {
//            paymentRequestService.sendInvoiceFile(msg.getPayload());
            msg.setProcessed(true);
        } catch (Exception e) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            if (msg.getRetryCount() > 3) {
                msg.setProcessed(true); // Optionally move to DeadLetterQueue
            } else {
                msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(10));
            }
        }
        messageQueueRepository.save(msg);
    }
}


