package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.dto.UserCreateDto;
import za.co.raretag.mawabes.dto.UserDto;
import za.co.raretag.mawabes.dto.UserUpdateDto;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.exception.DoesNotExist;

import java.util.List;

public interface UserDao {
    boolean authenticate(UserDto userDto) throws DoesNotExist;
    UserDto create(UserCreateDto userDto);
    UserDto update(UserUpdateDto userUpdateDto);
    UserDto updatePassword(UserUpdateDto userUpdateDto);
    UserDto getUserById(String id);
    List<UserDto> getAll();
    List<UserRoleEntity> getRoles(String user);
}
