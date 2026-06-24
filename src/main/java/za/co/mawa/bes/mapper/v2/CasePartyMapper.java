package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CasePartyEntity;
import za.co.mawa.bes.dto.v2.CasePartyCreateRequestDto;
import za.co.mawa.bes.dto.v2.CasePartyResponseDto;
import za.co.mawa.bes.dto.v2.CasePartyUpdateRequestDto;

@Component
public class CasePartyMapper {

    public CasePartyResponseDto toResponse(CasePartyEntity entity) {
        if (entity == null) {
            return null;
        }

        return CasePartyResponseDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseId())
                .partnerId(entity.getPartnerId())
                .partyName(entity.getPartyName())
                .partyType(entity.getPartyType())
                .idNumber(entity.getIdNumber())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .attorneyFirm(entity.getAttorneyFirm())
                .attorneyName(entity.getAttorneyName())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public CasePartyEntity toEntity(CasePartyCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CasePartyEntity.builder()
                .caseId(request.getCaseId())
                .partnerId(request.getPartnerId())
                .partyName(request.getPartyName())
                .partyType(request.getPartyType())
                .idNumber(request.getIdNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .attorneyFirm(request.getAttorneyFirm())
                .attorneyName(request.getAttorneyName())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(CasePartyEntity entity, CasePartyUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCaseId(request.getCaseId());
        entity.setPartnerId(request.getPartnerId());
        entity.setPartyName(request.getPartyName());
        entity.setPartyType(request.getPartyType());
        entity.setIdNumber(request.getIdNumber());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setAttorneyFirm(request.getAttorneyFirm());
        entity.setAttorneyName(request.getAttorneyName());
        entity.setNotes(request.getNotes());
    }
}
