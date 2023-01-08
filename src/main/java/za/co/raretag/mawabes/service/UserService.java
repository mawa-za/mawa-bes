package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.raretag.mawabes.dto.UserCreateDto;
import za.co.raretag.mawabes.dto.UserDto;
import za.co.raretag.mawabes.dto.UserUpdateDto;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.entity.UserRoleEntity;
import za.co.raretag.mawabes.dto.JwtRequest;
import za.co.raretag.mawabes.dao.UserDao;
import za.co.raretag.mawabes.exception.DoesNotExist;
import za.co.raretag.mawabes.repository.UserRepository;

import java.util.ArrayList;
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
    public boolean authenticate(UserDto userDto) throws DoesNotExist {
        boolean authenticated = false;
        UserEntity userEntity = userRepository.getById(userDto.getId());
        if (userEntity != null) {
            String storedPassword = new String(userEntity.getPassword());
            String enteredPassword = userDto.getPassword();
            authenticated = validatePassword(enteredPassword, storedPassword);
        } else {
            throw new DoesNotExist();
        }
        return authenticated;
    }

    @Override
    public UserDto create(UserCreateDto userCreateDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userCreateDto.getId());
        userEntity.setEmail(userCreateDto.getEmail());
        userEntity.setUserType(userCreateDto.getUserType());
        userEntity.setPassword(encryptionService.encrypt(userCreateDto.getPassword(), secret).getBytes());
        return entityToDto(userRepository.save(userEntity));
    }

    @Override
    public UserDto update(UserUpdateDto userUpdateDto) {
        UserEntity userEntity = userRepository.getById(userUpdateDto.getId());
        userEntity.setId(userUpdateDto.getId());
        return entityToDto(userRepository.save(userEntity));
    }

    @Override
    public UserDto updatePassword(UserUpdateDto userUpdateDto) {
        try {
            UserEntity userEntity = userRepository.getById(userUpdateDto.getId());
            userEntity.setPassword(encryptionService.encrypt(userUpdateDto.getPassword(), secret).getBytes());
            return entityToDto(userRepository.save(userEntity));
        } catch (Exception ex) {
            return null;
        }

    }

    @Override
    public UserDto getUserById(String id) {
        try {
            UserEntity userEntity = userRepository.getById(id);
            return entityToDto(userEntity);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<UserDto> getAll() {
        List<UserDto> userDtoList = new ArrayList<>();
        try {
            List<UserEntity> userEntities = userRepository.findAll();
            for (UserEntity userEntity : userEntities) {
                userDtoList.add(entityToDto(userEntity));
            }
        } catch (Exception ex) {

        }
        return userDtoList;
    }

    @Override
    public List<UserRoleEntity> getRoles(String user) {
        return null;
    }

    private boolean validatePassword(String enteredPassword, String storedPassword) {
        return encryptionService.encrypt(enteredPassword, secret).equals(storedPassword);
    }

    private UserDto entityToDto(UserEntity userEntity) {
        UserDto userDto = new UserDto();
        userDto.setId(userEntity.getId());
        return userDto;
    }

    private UserEntity dtoToEntity(UserDto userDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(userDto.getEmail());
        return userEntity;
    }
}
