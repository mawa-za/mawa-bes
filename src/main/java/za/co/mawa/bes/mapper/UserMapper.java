package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.dto.UserCreateRequestDto;
import za.co.mawa.bes.dto.UserResponseDto;
import za.co.mawa.bes.dto.UserUpdateRequestDto;

@Component
public class UserMapper {

    public UserResponseDto toResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .partner(entity.getPartner())
                .cellphone(entity.getCellphone())
                .email(entity.getEmail())
                .passwordStatus(entity.getPasswordStatus())
                .status(entity.getStatus())
                .statusReason(entity.getStatusReason())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .userType(entity.getUserType())
                .build();
    }

    public UserEntity toEntity(UserCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return UserEntity.builder()
                .username(request.getUsername())
                .partner(request.getPartner())
                .cellphone(request.getCellphone())
                .email(request.getEmail())
                .passwordStatus(request.getPasswordStatus())
                .status(request.getStatus())
                .statusReason(request.getStatusReason())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .userType(request.getUserType())
                .build();
    }

    public void updateEntity(UserEntity entity, UserUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setUsername(request.getUsername());
        entity.setPartner(request.getPartner());
        entity.setCellphone(request.getCellphone());
        entity.setEmail(request.getEmail());
        entity.setPasswordStatus(request.getPasswordStatus());
        entity.setStatus(request.getStatus());
        entity.setStatusReason(request.getStatusReason());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setUserType(request.getUserType());
    }
}
