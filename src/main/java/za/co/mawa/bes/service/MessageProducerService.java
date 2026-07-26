package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.MessageQueueInboundDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;

@Service
public class MessageProducerService {
    @Autowired
    private MessageQueueRepository messageQueueRepository;
    @Autowired
    private UserAccessService userAccessService;

    public void sendMessage(MessageQueueInboundDto messageQueueInboundDto) {
        if (messageQueueInboundDto != null && isExternalInitiation(messageQueueInboundDto.getType())) {
            userAccessService.assertExternalTransactionAllowed(messageQueueInboundDto.getType());
        }
        try {
            MessageQueueEntity messageQueueEntity = new MessageQueueEntity();
            messageQueueEntity.setReferenceId(messageQueueInboundDto.getReferenceId());
            messageQueueEntity.setReferenceNo(messageQueueInboundDto.getReferenceNo());
            messageQueueEntity.setType(messageQueueInboundDto.getType());
            messageQueueEntity.setPayload(messageQueueInboundDto.getPayload());
            messageQueueRepository.save(messageQueueEntity);
        } finally {

        }
    }

    public void sendMessageIfNotExists(MessageQueueInboundDto dto) {
        boolean exists = messageQueueRepository.existsByTypeAndReferenceId(
                dto.getType(),
                dto.getReferenceId()
        );

        if (exists) {
            return;
        }

        sendMessage(dto);
    }
    private boolean isExternalInitiation(String messageType) {
        if (messageType == null) return false;
        return "FNB-EFT-PAYMENT".equalsIgnoreCase(messageType)
                || "FNB-PAYROLL-PAYMENT".equalsIgnoreCase(messageType)
                || "XERO-INVOICE".equalsIgnoreCase(messageType)
                || "XERO-PAYMENT".equalsIgnoreCase(messageType)
                || "EXTERNAL-REFUND".equalsIgnoreCase(messageType);
    }

}
