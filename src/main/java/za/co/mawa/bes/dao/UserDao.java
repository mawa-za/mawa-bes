package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.UserCreateDto;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.dto.UserDto;
import za.co.mawa.bes.dto.UserUpdateDto;

import java.util.List;

public interface UserDao {
    boolean authenticate(UserDto userDto) throws DoesNotExist;
    UserDto create(UserCreateDto userDto);
    void reset(UserDto userDto);
    UserDto update(UserUpdateDto userUpdateDto);
    UserDto updatePassword(UserUpdateDto userUpdateDto);
    UserDto getUserById(String id);
    List<UserDto> getAll();
    List<UserRoleEntity> getRoles(String user);

}
