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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
    public UserDto create(UserCreateDto userCreateDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userCreateDto.getId());
        userEntity.setEmail(userCreateDto.getEmail());
        userEntity.setUserType(userCreateDto.getUserType());
        userEntity.setPassword(encryptionService.encrypt(userCreateDto.getPassword(), secret).getBytes());
        return entityToDto(userRepository.save(userEntity));
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
    public UserDto getUserById(String id) {
        try {
            UserEntity userEntity = userRepository.getById(id);
            return entityToDto(userEntity);
        } catch (EntityNotFoundException ex) {
            UserDto userDto = null;
            if (id.equals(ADMIN_USER)) {
                userDto = entityToDto(createAdminUser());
            }
            return userDto;
        } catch (Exception ex) {
            return null;
        }
    }

    private UserEntity createAdminUser() {
        UserEntity userEntity = new UserEntity();
        TestEntity testEntity = new TestEntity();
        try {
            testEntity.setUsername(ADMIN_USER);
            testEntity.setPassword(encryptionService.encrypt(DEFAULT_ADMIN_PASSWORD, secret).getBytes());
            testRepository.save(testEntity);

            userEntity.setId(ADMIN_USER);
            userEntity.setPassword(encryptionService.encrypt(DEFAULT_ADMIN_PASSWORD, secret).getBytes());
            userRepository.save(userEntity);
        } catch (Exception ex) {
           System.out.println(ex.getMessage());
        }
        return userEntity;
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
        userDto.setId(userEntity.getId());
        try {
            userDto.setPassword(new String(userEntity.getPassword(),"UTF-8"));
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
