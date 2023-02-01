package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.UserCreateDto;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.dto.UserDto;
import za.co.mawa.bes.dto.UserUpdateDto;

import java.util.List;

public interface UserDao {
    boolean authenticate(UserDto userDto) throws DoesNotExist;
    UserDto create(UserCreateDto userDto) throws Exception;
    void reset(UserDto userDto);
    UserDto update(UserUpdateDto userUpdateDto);
    UserDto updatePassword(UserUpdateDto userUpdateDto);
    UserDto getUserByName(String username) throws Exception;
    List<UserDto> getAll();
    List<UserRoleEntity> getRoles(String user);
    String getCurrentUser();

}
