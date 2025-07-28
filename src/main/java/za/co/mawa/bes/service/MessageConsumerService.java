package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageConsumerService {

    @Autowired
    private MessageQueueRepository messageQueueRepository;

    @Autowired
    TenantAdminService tenantAdminService;

    @Scheduled(fixedDelay = 5000)
    public void processAllTenants() {
        for (TenantDto tenant : tenantAdminService.getAll()) {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                List<MessageQueueEntity> messageQueueEntities = messageQueueRepository
                        .findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime.now());

                for (MessageQueueEntity msg : messageQueueEntities) {
                    try {
                        System.out.println("Tenant: " + tenant + " Payload: " + msg.getPayload());
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

            } catch (Exception e) {
                System.err.println("Error processing tenant " + tenant + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}

