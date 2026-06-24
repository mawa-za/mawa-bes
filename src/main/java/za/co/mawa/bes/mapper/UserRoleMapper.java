package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.UserRoleCreateRequestDto;
import za.co.mawa.bes.dto.UserRoleResponseDto;
import za.co.mawa.bes.dto.UserRoleUpdateRequestDto;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;

@Component
public class UserRoleMapper {
    public UserRoleResponseDto toResponse(UserRoleEntity entity) {
        if (entity == null) return null;
        return UserRoleResponseDto.builder()
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }
    public UserRoleEntity toEntity(UserRoleCreateRequestDto request) {
        if (request == null) return null;
        UserRoleEntity entity = new UserRoleEntity();
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        return entity;
    }
    public void updateEntity(UserRoleEntity entity, UserRoleUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
