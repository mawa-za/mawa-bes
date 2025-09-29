package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.MessageQueueInboundDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;

@Service
public class MessageProducerService {
    @Autowired
    private MessageQueueRepository messageQueueRepository;

    public void sendMessage(MessageQueueInboundDto messageQueueInboundDto) {
        try {
            MessageQueueEntity messageQueueEntity = new MessageQueueEntity();
            messageQueueEntity.setType(messageQueueInboundDto.getType());
            messageQueueEntity.setPayload(messageQueueInboundDto.getPayload());
            messageQueueRepository.save(messageQueueEntity);
        } finally {

        }
    }
}
