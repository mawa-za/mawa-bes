package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralPackageEntity;
import za.co.mawa.bes.dto.v2.FuneralPackageCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPackageResponseDto;
import za.co.mawa.bes.dto.v2.FuneralPackageUpdateRequestDto;

@Component
public class FuneralPackageMapper {

    public FuneralPackageResponseDto toResponse(FuneralPackageEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralPackageResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .basePriceCents(entity.getBasePriceCents())
                .inclusionsJson(entity.getInclusionsJson())
                .active(entity.getActive())
                .build();
    }

    public FuneralPackageEntity toEntity(FuneralPackageCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralPackageEntity.builder()
                .name(request.getName())
                .basePriceCents(request.getBasePriceCents())
                .inclusionsJson(request.getInclusionsJson())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(FuneralPackageEntity entity, FuneralPackageUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setBasePriceCents(request.getBasePriceCents());
        entity.setInclusionsJson(request.getInclusionsJson());
        entity.setActive(request.getActive());
    }
}
