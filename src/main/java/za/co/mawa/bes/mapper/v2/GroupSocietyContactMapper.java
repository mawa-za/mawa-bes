package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;
import za.co.mawa.bes.dto.v2.GroupSocietyContactCreateRequestDto;
import za.co.mawa.bes.dto.v2.GroupSocietyContactResponseDto;
import za.co.mawa.bes.dto.v2.GroupSocietyContactUpdateRequestDto;

@Component
public class GroupSocietyContactMapper {

    public GroupSocietyContactResponseDto toResponse(GroupSocietyContactEntity entity) {
        if (entity == null) {
            return null;
        }

        return GroupSocietyContactResponseDto.builder()
                .id(entity.getId())
                .groupSocietyId(entity.getGroupSocietyId())
                .contactName(entity.getContactName())
                .role(entity.getRole())
                .mobileNo(entity.getMobileNo())
                .email(entity.getEmail())
                .primaryContact(entity.getPrimaryContact())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public GroupSocietyContactEntity toEntity(GroupSocietyContactCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return GroupSocietyContactEntity.builder()
                .groupSocietyId(request.getGroupSocietyId())
                .contactName(request.getContactName())
                .role(request.getRole())
                .mobileNo(request.getMobileNo())
                .email(request.getEmail())
                .primaryContact(request.getPrimaryContact())
                .build();
    }

    public void updateEntity(GroupSocietyContactEntity entity, GroupSocietyContactUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setGroupSocietyId(request.getGroupSocietyId());
        entity.setContactName(request.getContactName());
        entity.setRole(request.getRole());
        entity.setMobileNo(request.getMobileNo());
        entity.setEmail(request.getEmail());
        entity.setPrimaryContact(request.getPrimaryContact());
    }
}
