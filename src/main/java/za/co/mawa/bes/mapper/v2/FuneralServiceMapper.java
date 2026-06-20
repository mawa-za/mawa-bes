package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;
import za.co.mawa.bes.dto.v2.FuneralServiceCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralServiceResponseDto;
import za.co.mawa.bes.dto.v2.FuneralServiceUpdateRequestDto;

@Component
public class FuneralServiceMapper {

    public FuneralServiceResponseDto toResponse(FuneralServiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralServiceResponseDto.builder()
                .id(entity.getId())
                .mortuaryInventoryId(entity.getMortuaryInventoryId())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .deceasedName(entity.getDeceasedName())
                .deceasedIdentityNumber(entity.getDeceasedIdentityNumber())
                .packageId(entity.getPackageId())
                .familyRepId(entity.getFamilyRepId())
                .funeralDate(entity.getFuneralDate())
                .funeralArea(entity.getFuneralArea())
                .totalAmountCents(entity.getTotalAmountCents())
                .extrasJson(entity.getExtrasJson())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FuneralServiceEntity toEntity(FuneralServiceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralServiceEntity.builder()
                .mortuaryInventoryId(request.getMortuaryInventoryId())
                .deceasedPartnerId(request.getDeceasedPartnerId())
                .deceasedName(request.getDeceasedName())
                .deceasedIdentityNumber(request.getDeceasedIdentityNumber())
                .packageId(request.getPackageId())
                .familyRepId(request.getFamilyRepId())
                .funeralDate(request.getFuneralDate())
                .funeralArea(request.getFuneralArea())
                .totalAmountCents(request.getTotalAmountCents())
                .extrasJson(request.getExtrasJson())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(FuneralServiceEntity entity, FuneralServiceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setMortuaryInventoryId(request.getMortuaryInventoryId());
        entity.setDeceasedPartnerId(request.getDeceasedPartnerId());
        entity.setDeceasedName(request.getDeceasedName());
        entity.setDeceasedIdentityNumber(request.getDeceasedIdentityNumber());
        entity.setPackageId(request.getPackageId());
        entity.setFamilyRepId(request.getFamilyRepId());
        entity.setFuneralDate(request.getFuneralDate());
        entity.setFuneralArea(request.getFuneralArea());
        entity.setTotalAmountCents(request.getTotalAmountCents());
        entity.setExtrasJson(request.getExtrasJson());
        entity.setStatus(request.getStatus());
    }
}
