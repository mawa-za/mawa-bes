package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.TestEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.TestRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.dto.UserCreateDto;
import za.co.mawa.bes.dto.UserDto;
import za.co.mawa.bes.dto.UserUpdateDto;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.dao.UserDao;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.utils.Constant;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserService implements UserDao {
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserRepository userRepository;

    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    EncryptionService encryptionService;
    private String secret;
    public static final String ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";
    @Autowired
    private TestRepository testRepository;

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
    public UserDto create(UserCreateDto userCreateDto) throws Exception {
        try {
            UserEntity userEntity = new UserEntity();
//            userEntity.setId(userCreateDto.getId());
            userEntity.setUsername(userCreateDto.getUsername());
            userEntity.setEmail(userCreateDto.getEmail());
            userEntity.setUserType(userCreateDto.getUserType());
            userEntity.setValidFrom(new Date());
            userEntity.setValidTo(new Date());
            userEntity.setPassword(encryptionService.encrypt(userCreateDto.getPassword(), secret).getBytes());
            return entityToDto(userRepository.save(userEntity));
        } catch (Exception exception) {
            throw new Exception();
        }
    }

    @Override
    public void reset(UserDto userDto) {
        UserEntity userEntity = userRepository.getById(userDto.getId());
//        userEntity.setId(userUpdateDto.getId());
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
    public UserDto getUserByName(String username) throws Exception {
        try {
            UserEntity userEntity = userRepository.getByName(username);
            if (userEntity == null){
                UserDto userDto = null;
                if (username.equals(ADMIN_USER)) {
                    UserCreateDto userCreateDto = new UserCreateDto();
                    userCreateDto.setUsername(ADMIN_USER);
                    userCreateDto.setPassword(DEFAULT_ADMIN_PASSWORD);
                    userDto = create(userCreateDto);
                }
                return userDto;
            }else{
                return entityToDto(userEntity);
            }

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
        return userRoleRepository.findUserRoles(user);
    }

    @Override
    public String getCurrentUser() {
        return UserContext.getCurrentUser();
    }

    private boolean validatePassword(String enteredPassword, String storedPassword) {
        return encryptionService.encrypt(enteredPassword, secret).equals(storedPassword);
    }

    private UserDto entityToDto(UserEntity userEntity) {
        UserDto userDto = new UserDto();
        userDto.setUsername(userEntity.getUsername());
        try {
            userDto.setPassword(new String(userEntity.getPassword(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        userDto.setEmail(userEntity.getEmail());
        return userDto;
    }

    private UserEntity dtoToEntity(UserDto userDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(userDto.getEmail());
        return userEntity;
    }
}
