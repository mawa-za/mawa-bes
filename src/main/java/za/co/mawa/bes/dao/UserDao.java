package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.user.*;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.UserExistException;

import java.util.List;

public interface UserDao {
    boolean authenticate(UserDto userDto) throws DoesNotExist;
    UserDto create(UserCreateDto userDto) throws UserExistException;
    void reset(UserDto userDto);
    UserDto update(UserUpdateDto userUpdateDto);
    UserDto updatePassword(UserUpdateDto userUpdateDto);
    UserDto getUserByName(String username) throws Exception;
    List<UserDto> getAll(UserQueryDto query);
    List<String> getRoles(String user);
    String getCurrentUser();
    void addRole(UserRoleDto userRoleDto) throws Exception;
    boolean lockuser(String id,String statusReason) throws Exception;
    boolean unlockuser(String id) throws Exception;
    boolean deleteRole(UserRolePKEntity entityPk) throws Exception;
    String resetUser(String id) throws Exception;
    boolean deleteUser(String id) throws Exception;
    boolean editUser(String id,UserEditDto edit) throws Exception;
    UserDto getUserById(String id) throws Exception;
    PartnerDto getPartner(String user);
}
