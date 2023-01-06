package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.dao.UserDao;
import za.co.raretag.mawabes.exception.DoesNotExist;
import za.co.raretag.mawabes.repository.UserRepository;

import java.util.List;

@Service
public class UserService implements UserDao {
    @Autowired
    UserRepository userRepository;
    @Override
    public boolean authenticate(JwtRequest jwtRequest) throws DoesNotExist {
        boolean authenticated = false;
        UserEntity userEntity = userRepository.getById(jwtRequest.getUsername());
        if (userEntity != null) {
            String storedPassword = new String(userEntity.getPassword());
            String enteredPassword = jwtRequest.getPassword();
//            authenticated = validatePassword(enteredPassword, storedPassword);
        } else {
            throw new DoesNotExist();
        }
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
