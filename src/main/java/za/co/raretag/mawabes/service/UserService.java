package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.dao.UserDao;
import za.co.raretag.mawabes.exception.DoesNotExist;
import za.co.raretag.mawabes.repository.UserRepository;

import java.util.List;

@Component
public class UserService implements UserDao {
    @Autowired
    UserRepository userRepository;
    @Autowired
    EncryptionService encryptionService;

    private String secret;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public boolean authenticate(JwtRequest jwtRequest) throws DoesNotExist {
        boolean authenticated = false;
        UserEntity userEntity = userRepository.getById(jwtRequest.getUsername());
        if (userEntity != null) {
            String storedPassword = new String(userEntity.getPassword());
            String enteredPassword = jwtRequest.getPassword();
            authenticated = validatePassword(enteredPassword, storedPassword);
        } else {
            throw new DoesNotExist();
        }
        return authenticated;
    }

    @Override
    public boolean create(UserEntity userEntity) {
        String pass = userEntity.getPassword().toString();
        userEntity.setPassword(encryptionService.encrypt(pass, secret).getBytes());
        return false;
    }

    @Override
    public UserEntity getUserById(String id) {
        try {
            UserEntity user = userRepository.getById(id);
            return user;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<UserEntity> getAll() {
        try {

            List<UserEntity> userEntities = userRepository.findAll();
            return userEntities;
        } catch (Exception ex) {
            return null;
        }

    }

    @Override
    public List<UserRoleEntity> getRoles(String user) {
        return null;
    }

    private boolean validatePassword(String enteredPassword, String storedPassword) {
        return encryptionService.encrypt(enteredPassword, secret).equals(storedPassword);
    }
}
