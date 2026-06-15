package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.dto.MessageQueueCreateRequestDto;
import za.co.mawa.bes.dto.MessageQueueResponseDto;
import za.co.mawa.bes.dto.MessageQueueUpdateRequestDto;

@Component
public class MessageQueueMapper {

    public MessageQueueResponseDto toResponse(MessageQueueEntity entity) {
        if (entity == null) {
            return null;
        }

        return MessageQueueResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .payload(entity.getPayload())
                .processed(entity.getProcessed())
                .retryCount(entity.getRetryCount())
                .nextAttemptAt(entity.getNextAttemptAt())
                .build();
    }

    public MessageQueueEntity toEntity(MessageQueueCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MessageQueueEntity.builder()
                .type(request.getType())
                .referenceId(request.getReferenceId())
                .referenceNo(request.getReferenceNo())
                .payload(request.getPayload())
                .processed(request.getProcessed())
                .retryCount(request.getRetryCount())
                .nextAttemptAt(request.getNextAttemptAt())
                .build();
    }

    public void updateEntity(MessageQueueEntity entity, MessageQueueUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setType(request.getType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setPayload(request.getPayload());
        entity.setProcessed(request.getProcessed());
        entity.setRetryCount(request.getRetryCount());
        entity.setNextAttemptAt(request.getNextAttemptAt());
    }
}
