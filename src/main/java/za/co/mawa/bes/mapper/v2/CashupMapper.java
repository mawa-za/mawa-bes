package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.dto.v2.CashupCreateRequestDto;
import za.co.mawa.bes.dto.v2.CashupResponseDto;
import za.co.mawa.bes.dto.v2.CashupUpdateRequestDto;

@Component
public class CashupMapper {

    public CashupResponseDto toResponse(CashupEntity entity) {
        if (entity == null) {
            return null;
        }

        return CashupResponseDto.builder()
                .id(entity.getId())
                .cashupNo(entity.getCashupNo())
                .deviceId(entity.getDeviceId())
                .userId(entity.getUserId())
                .cashupDate(entity.getCashupDate())
                .totalCents(entity.getTotalCents())
                .receiptCount(entity.getReceiptCount())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .syncedAt(entity.getSyncedAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public CashupEntity toEntity(CashupCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupEntity.builder()
                .cashupNo(request.getCashupNo())
                .deviceId(request.getDeviceId())
                .userId(request.getUserId())
                .cashupDate(request.getCashupDate())
                .totalCents(request.getTotalCents())
                .receiptCount(request.getReceiptCount())
                .status(request.getStatus())
                .notes(request.getNotes())
                .syncedAt(request.getSyncedAt())
                .build();
    }

    public void updateEntity(CashupEntity entity, CashupUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCashupNo(request.getCashupNo());
        entity.setDeviceId(request.getDeviceId());
        entity.setUserId(request.getUserId());
        entity.setCashupDate(request.getCashupDate());
        entity.setTotalCents(request.getTotalCents());
        entity.setReceiptCount(request.getReceiptCount());
        entity.setStatus(request.getStatus());
        entity.setNotes(request.getNotes());
        entity.setSyncedAt(request.getSyncedAt());
    }
}
