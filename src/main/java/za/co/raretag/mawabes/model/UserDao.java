package za.co.raretag.mawabes.model;

import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;

import java.util.List;

public interface UserDao {
    boolean authenticate(JwtRequest jwtRequest);
    boolean create(UserEntity userEntity);
    List<UserRoleEntity> getRoles(String user);
}
