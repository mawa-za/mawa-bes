package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.exception.DoesNotExist;

import java.util.List;

public interface UserDao {
    boolean authenticate(JwtRequest jwtRequest) throws DoesNotExist;
    boolean create(UserEntity userEntity);
    UserEntity getUserById(String id);
    List<UserEntity> getAll();
    List<UserRoleEntity> getRoles(String user);
}
