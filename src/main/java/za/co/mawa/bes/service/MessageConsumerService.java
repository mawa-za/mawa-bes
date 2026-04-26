package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import org.hibernate.loader.access.BaseNaturalIdLoadAccessImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkInboundDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.fnb.BankPaymentService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.PaymentInformation;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.TransactionAttribute;
import za.co.mawa.bes.utils.TransactionLinkType;
import za.co.mawa.bes.utils.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageConsumerService {

    @Autowired
    MessageQueueRepository messageQueueRepository;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    BankPaymentService bankPaymentService;
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    TransactionLinkService transactionLinkService;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionAttributeService transactionAttributeService;
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
                                for (PaymentInformation paymentInformation : bankPaymentRequest.getPaymentInformation()) {
                                    TransactionEntity transactionEntity = transactionRepository.findTransactionByTypeNumber(TransactionType.PAYMENT_REQUEST,paymentInformation.getPaymentInformationId());
                                    paymentRequestService.sendToBank(transactionEntity.getId());
                                    TransactionAttributeDto attribute = new TransactionAttributeDto();
                                    attribute.setTransaction(transactionEntity.getId());
                                    attribute.setAttribute(TransactionAttribute.BANK_INSTRUCTION_ID);
                                    attribute.setValue(instructionId);
                                    transactionAttributeService.add(attribute);
                                }
                                msg.setProcessed(true);
                                break;
                            case "INVOICE-EMAIL":
                                paymentRequestService.sendInvoiceFile(msg.getPayload());
                                msg.setProcessed(true);
                                break;
                            default:
                                // code block
                        }

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

            } catch (Exception e) {
                System.err.println("Error processing tenant " + tenant + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void sendInvoice(MessageQueueEntity msg) {
        try {
            paymentRequestService.sendInvoiceFile(msg.getPayload());
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


