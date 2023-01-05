package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.model.JwtRequest;
import za.co.raretag.mawabes.model.UserDao;
import za.co.raretag.mawabes.repository.UserRepository;

import java.util.List;

@Service
public class UserService implements UserDao {
    @Autowired
    UserRepository userRepository;
    @Override
    public boolean authenticate(JwtRequest jwtRequest) {
        userRepository.getById(jwtRequest.getUsername());
        return true;
    }

    @Override
    public boolean create(UserEntity userEntity) {
        return false;
    }

    @Override
    public List<UserRoleEntity> getRoles(String user) {
        return null;
    }
}
