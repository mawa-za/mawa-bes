package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralPickupRequestEntity;
import za.co.mawa.bes.dto.v2.FuneralPickupRequestCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPickupRequestResponseDto;
import za.co.mawa.bes.dto.v2.FuneralPickupRequestUpdateRequestDto;

@Component
public class FuneralPickupRequestMapper {

    public FuneralPickupRequestResponseDto toResponse(FuneralPickupRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralPickupRequestResponseDto.builder()
                .id(entity.getId())
                .deceasedName(entity.getDeceasedName())
                .pickupLocation(entity.getPickupLocation())
                .contactPerson(entity.getContactPerson())
                .contactNumber(entity.getContactNumber())
                .assignedStaffId(entity.getAssignedStaffId())
                .completionTime(entity.getCompletionTime())
                .status(entity.getStatus())
                .mortuaryInventoryId(entity.getMortuaryInventoryId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FuneralPickupRequestEntity toEntity(FuneralPickupRequestCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralPickupRequestEntity.builder()
                .deceasedName(request.getDeceasedName())
                .pickupLocation(request.getPickupLocation())
                .contactPerson(request.getContactPerson())
                .contactNumber(request.getContactNumber())
                .assignedStaffId(request.getAssignedStaffId())
                .completionTime(request.getCompletionTime())
                .status(request.getStatus())
                .mortuaryInventoryId(request.getMortuaryInventoryId())
                .build();
    }

    public void updateEntity(FuneralPickupRequestEntity entity, FuneralPickupRequestUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setDeceasedName(request.getDeceasedName());
        entity.setPickupLocation(request.getPickupLocation());
        entity.setContactPerson(request.getContactPerson());
        entity.setContactNumber(request.getContactNumber());
        entity.setAssignedStaffId(request.getAssignedStaffId());
        entity.setCompletionTime(request.getCompletionTime());
        entity.setStatus(request.getStatus());
        entity.setMortuaryInventoryId(request.getMortuaryInventoryId());
    }
}
