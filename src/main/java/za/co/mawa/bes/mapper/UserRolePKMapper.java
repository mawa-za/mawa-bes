package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.dto.UserRolePKCreateRequestDto;
import za.co.mawa.bes.dto.UserRolePKResponseDto;
import za.co.mawa.bes.dto.UserRolePKUpdateRequestDto;

@Component
public class UserRolePKMapper {

    public UserRolePKResponseDto toResponse(UserRolePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserRolePKResponseDto.builder()
                .user(entity.getUser())
                .role(entity.getRole())
                .build();
    }

    public UserRolePKEntity toEntity(UserRolePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return UserRolePKEntity.builder()
                .user(request.getUser())
                .role(request.getRole())
                .build();
    }

    public void updateEntity(UserRolePKEntity entity, UserRolePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUser(request.getUser());
        entity.setRole(request.getRole());
    }
}
