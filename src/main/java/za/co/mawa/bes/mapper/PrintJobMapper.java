package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PrintJobEntity;
import za.co.mawa.bes.dto.PrintJobCreateRequestDto;
import za.co.mawa.bes.dto.PrintJobResponseDto;
import za.co.mawa.bes.dto.PrintJobUpdateRequestDto;

@Component
public class PrintJobMapper {

    public PrintJobResponseDto toResponse(PrintJobEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrintJobResponseDto.builder()
                .id(entity.getId())
                .creationTimestamp(entity.getCreationTimestamp())
                .printerId(entity.getPrinterId())
                .content(entity.getContent())
                .completed(entity.isCompleted())
                .completedTimestamp(entity.getCompletedTimestamp())
                .build();
    }

    public PrintJobEntity toEntity(PrintJobCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PrintJobEntity.builder()
                .creationTimestamp(request.getCreationTimestamp())
                .printerId(request.getPrinterId())
                .content(request.getContent())
                .completed(request.isCompleted())
                .completedTimestamp(request.getCompletedTimestamp())
                .build();
    }

    public void updateEntity(PrintJobEntity entity, PrintJobUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCreationTimestamp(request.getCreationTimestamp());
        entity.setPrinterId(request.getPrinterId());
        entity.setContent(request.getContent());
        entity.setCompleted(request.isCompleted());
        entity.setCompletedTimestamp(request.getCompletedTimestamp());
    }
}
