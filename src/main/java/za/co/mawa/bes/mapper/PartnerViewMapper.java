package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.dto.PartnerViewCreateRequestDto;
import za.co.mawa.bes.dto.PartnerViewResponseDto;
import za.co.mawa.bes.dto.PartnerViewUpdateRequestDto;

@Component
public class PartnerViewMapper {

    public PartnerViewResponseDto toResponse(PartnerViewEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerViewResponseDto.builder()
                .partnerId(entity.getPartnerId())
                .partnerNo(entity.getPartnerNo())
                .partnerType(entity.getPartnerType())
                .partnerRole(entity.getPartnerRole())
                .identityType(entity.getIdentityType())
                .identityNumber(entity.getIdentityNumber())
                .name1(entity.getName1())
                .name2(entity.getName2())
                .name3(entity.getName3())
                .birthDate(entity.getBirthDate())
                .gender(entity.getGender())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .maritalStatus(entity.getMaritalStatus())
                .build();
    }

    public PartnerViewEntity toEntity(PartnerViewCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerViewEntity.builder()
                .partnerId(request.getPartnerId())
                .partnerNo(request.getPartnerNo())
                .partnerType(request.getPartnerType())
                .partnerRole(request.getPartnerRole())
                .identityType(request.getIdentityType())
                .identityNumber(request.getIdentityNumber())
                .name1(request.getName1())
                .name2(request.getName2())
                .name3(request.getName3())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .title(request.getTitle())
                .status(request.getStatus())
                .maritalStatus(request.getMaritalStatus())
                .build();
    }

    public void updateEntity(PartnerViewEntity entity, PartnerViewUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartnerId(request.getPartnerId());
        entity.setPartnerNo(request.getPartnerNo());
        entity.setPartnerType(request.getPartnerType());
        entity.setPartnerRole(request.getPartnerRole());
        entity.setIdentityType(request.getIdentityType());
        entity.setIdentityNumber(request.getIdentityNumber());
        entity.setName1(request.getName1());
        entity.setName2(request.getName2());
        entity.setName3(request.getName3());
        entity.setBirthDate(request.getBirthDate());
        entity.setGender(request.getGender());
        entity.setTitle(request.getTitle());
        entity.setStatus(request.getStatus());
        entity.setMaritalStatus(request.getMaritalStatus());
    }
}
