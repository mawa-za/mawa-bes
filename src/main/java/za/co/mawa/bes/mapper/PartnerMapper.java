package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.dto.PartnerCreateRequestDto;
import za.co.mawa.bes.dto.PartnerResponseDto;
import za.co.mawa.bes.dto.PartnerUpdateRequestDto;

@Component
public class PartnerMapper {

    public PartnerResponseDto toResponse(PartnerEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerResponseDto.builder()
                .id(entity.getId())
                .no(entity.getNo())
                .birthDate(entity.getBirthDate())
                .gender(entity.getGender())
                .language(entity.getLanguage())
                .maritalStatus(entity.getMaritalStatus())
                .name1(entity.getName1())
                .name2(entity.getName2())
                .name3(entity.getName3())
                .title(entity.getTitle())
                .type(entity.getType())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .statusReason(entity.getStatusReason())
                .createdBy(entity.getCreatedBy())
                .creationDate(entity.getCreationDate())
                .build();
    }

    public PartnerEntity toEntity(PartnerCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerEntity.builder()
                .no(request.getNo())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .language(request.getLanguage())
                .maritalStatus(request.getMaritalStatus())
                .name1(request.getName1())
                .name2(request.getName2())
                .name3(request.getName3())
                .title(request.getTitle())
                .type(request.getType())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .statusReason(request.getStatusReason())
                .creationDate(request.getCreationDate())
                .build();
    }

    public void updateEntity(PartnerEntity entity, PartnerUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setNo(request.getNo());
        entity.setBirthDate(request.getBirthDate());
        entity.setGender(request.getGender());
        entity.setLanguage(request.getLanguage());
        entity.setMaritalStatus(request.getMaritalStatus());
        entity.setName1(request.getName1());
        entity.setName2(request.getName2());
        entity.setName3(request.getName3());
        entity.setTitle(request.getTitle());
        entity.setType(request.getType());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
        entity.setStatusReason(request.getStatusReason());
        entity.setCreationDate(request.getCreationDate());
    }
}
