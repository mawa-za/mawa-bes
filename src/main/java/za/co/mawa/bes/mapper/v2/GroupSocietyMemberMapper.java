package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;
import za.co.mawa.bes.dto.v2.GroupSocietyMemberCreateRequestDto;
import za.co.mawa.bes.dto.v2.GroupSocietyMemberResponseDto;
import za.co.mawa.bes.dto.v2.GroupSocietyMemberUpdateRequestDto;

@Component
public class GroupSocietyMemberMapper {

    public GroupSocietyMemberResponseDto toResponse(GroupSocietyMemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return GroupSocietyMemberResponseDto.builder()
                .id(entity.getId())
                .groupSocietyId(entity.getGroupSocietyId())
                .memberId(entity.getMemberId())
                .membershipId(entity.getMembershipId())
                .employeeNo(entity.getEmployeeNo())
                .externalRef(entity.getExternalRef())
                .joinDate(entity.getJoinDate())
                .exitDate(entity.getExitDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public GroupSocietyMemberEntity toEntity(GroupSocietyMemberCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return GroupSocietyMemberEntity.builder()
                .groupSocietyId(request.getGroupSocietyId())
                .memberId(request.getMemberId())
                .membershipId(request.getMembershipId())
                .employeeNo(request.getEmployeeNo())
                .externalRef(request.getExternalRef())
                .joinDate(request.getJoinDate())
                .exitDate(request.getExitDate())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(GroupSocietyMemberEntity entity, GroupSocietyMemberUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setGroupSocietyId(request.getGroupSocietyId());
        entity.setMemberId(request.getMemberId());
        entity.setMembershipId(request.getMembershipId());
        entity.setEmployeeNo(request.getEmployeeNo());
        entity.setExternalRef(request.getExternalRef());
        entity.setJoinDate(request.getJoinDate());
        entity.setExitDate(request.getExitDate());
        entity.setStatus(request.getStatus());
    }
}
