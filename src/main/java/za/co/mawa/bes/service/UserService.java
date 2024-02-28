package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.user.*;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.UserExistException;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.dao.UserDao;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.service.PartnerService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserService implements UserDao {
    //    @Autowired
//    PartnerService partnerService;
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserRepository userRepository;
    @Autowired
    EmailService emailService;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    SimpleKeyGenerator keyGenerator;
    @Autowired
    PartnerService partnerService;
    private String secret;
    public static final String SYSTEM_USER = "system";
    public static final String DEFAULT_SYSTEM_PASSWORD = "system";

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
    public UserDto create(UserCreateDto userCreateDto) throws UserExistException {
        UserEntity userFound = userRepository.getByName(userCreateDto.getUsername());
        if (userFound != null) {
            throw new UserExistException("Username already exist");
        }
        userFound = userRepository.getByEmail(userCreateDto.getEmail());
        if (userFound != null) {
            throw new UserExistException("Email already assigned to user");
        }
        userFound = userRepository.getByCellphone(userCreateDto.getCellphone());
        if (userFound != null) {
            throw new UserExistException("Cellphone number already assigned to user");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setPartner(userCreateDto.getPartnerId());
        userEntity.setUsername(userCreateDto.getUsername());
        userEntity.setEmail(userCreateDto.getEmail());
        userEntity.setCellphone(userCreateDto.getCellphone());
        userEntity.setUserType(userCreateDto.getUserType().toUpperCase());
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setPasswordStatus(PasswordStatus.INITIAL);
        userEntity.setValidFrom(new Date());
        userEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
        if (userCreateDto.getPassword() == null) {
            userCreateDto.setPassword(keyGenerator.generatePassword());
        }
        userEntity.setPassword(encryptionService.encrypt(userCreateDto.getPassword(), secret).getBytes());
        UserDto userDto = entityToDto(userRepository.save(userEntity));
        EmailDto emailDto = new EmailDto();
        emailDto.setTo(userEntity.getEmail());
        emailDto.setSubject("New user");
        emailDto.setTemplate("new-user");
        List<PropertyDto> props = new ArrayList<>();
        props.add(new PropertyDto(HtmlTemplateVariableKey.USER_NAME, userCreateDto.getUsername()));
        props.add(new PropertyDto(HtmlTemplateVariableKey.USER_PASSWORD, userCreateDto.getPassword()));
        emailDto.setProperties(props);
        try {
            emailService.send(emailDto);
        } catch (Exception ex) {

        }
        return userDto;
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
        //Utils.generateRandomPassword();
    }

    @Override
    public UserDto getUserByName(String username) throws Exception {
        try {
//            userRepository.findAll();
            UserEntity userEntity = userRepository.getByName(username);
            if (userEntity == null) {
                UserDto userDto = null;
                if (username.equals(SYSTEM_USER)) {
                    PartnerCreateDto partnerCreateDto = new PartnerCreateDto();
                    partnerCreateDto.setType(PartnerType.INDIVIDUAL);
                    partnerCreateDto.setName1("SYSTEM");
                    partnerCreateDto.setName2("USER");
                    PartnerDto partnerDto = partnerService.create(partnerCreateDto);
                    UserCreateDto userCreateDto = new UserCreateDto();
                    userCreateDto.setPartnerId(partnerDto.getId());
                    userCreateDto.setUsername(SYSTEM_USER);
                    userCreateDto.setPassword(DEFAULT_SYSTEM_PASSWORD);
                    userCreateDto.setUserType(UserType.ADMIN);
                    userDto = create(userCreateDto);
                    UserRoleDto userRoleDto = new UserRoleDto();
                    userRoleDto.setUser(userDto.getId());
                    userRoleDto.setRole("SYSTEM");
                    addRole(userRoleDto);
                }
                return userDto;
            } else {
                return entityToDto(userEntity);
            }

        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<UserDto> getAll(UserQueryDto query) {
        List<UserDto> userDtoList = new ArrayList<>();
        try {
            Sort sort = Sort.by("id").descending();
            List<UserEntity> userEntities = userRepository.findAll(findByCriteria(query), sort);
            for (UserEntity userEntity : userEntities) {
                UserDto user = new UserDto();
                user = entityToDto(userEntity);
                user.setPassword(null);
                userDtoList.add(user);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return userDtoList;
    }

    @Override
    public List<String> getRoles(String user) {
        List<String> roleList = new ArrayList<>();
        List<UserRoleEntity> userRoleEntities = userRoleRepository.findUserRoles(user);
        for (UserRoleEntity userRoleEntity : userRoleEntities) {
            roleList.add(userRoleEntity.getUserRolePKEntity().getRole());
        }
        return roleList;
    }

    @Override
    public String getCurrentUser() {
        return UserContext.getCurrentUser();
    }

    @Override
    public void addRole(UserRoleDto userRoleDto) throws Exception {
        try {
            UserRolePKEntity userRolePKEntity = new UserRolePKEntity();
            userRolePKEntity.setUser(userRoleDto.getUser());
            userRolePKEntity.setRole(userRoleDto.getRole());
            UserRoleEntity userRoleEntity = new UserRoleEntity();
            userRoleEntity.setUserRolePKEntity(userRolePKEntity);
            userRoleEntity.setValidFrom(new Date());
            userRoleEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            userRoleRepository.save(userRoleEntity);
        } catch (Exception exception) {
            throw new Exception();
        }
    }

    @Override
    public boolean lockuser(String id, String statusReason) throws Exception {
        try {
            UserEntity user = userRepository.getById(id);
            user.setStatus(UserStatus.LOCKED);
            user.setStatusReason(statusReason);
            userRepository.save(user);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean unlockuser(String id) throws Exception {
        try {
            UserEntity user = userRepository.getById(id);
            user.setStatus(UserStatus.ACTIVE);
            user.setStatusReason("");
            userRepository.save(user);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean deleteRole(UserRolePKEntity entityPk) throws Exception {
        try {
            userRoleRepository.deleteById(entityPk);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public String resetUser(String id) throws Exception {
        try {
            String password = keyGenerator.generatePassword();
            UserEntity userEntity = userRepository.getById(id);
//            PartnerDto partnerDto = partnerService.get(userEntity.getPartner());
            userEntity.setPassword(encryptionService.encrypt(password, secret).getBytes());
            userRepository.save(userEntity);

            EmailDto emailDto = new EmailDto();
            emailDto.setTo(userEntity.getEmail());
            emailDto.setSubject("Password Reset");
            emailDto.setTemplate("password-reset");
            List<PropertyDto> props = new ArrayList<>();
//            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_FIRST_NAME,partnerDto.getName2()));
            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_PASSWORD, password));
            emailDto.setProperties(props);
            emailService.send(emailDto);
            return password;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean deleteUser(String id) throws Exception {
        try {
            userRepository.deleteById(id);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean editUser(String id, UserEditDto edit) throws Exception {
        UserEntity user = userRepository.getById(id);

        if (user != null) {
            if (edit.getEmail() != null && edit.getEmail() != "") {
                UserQueryDto queryDto = new UserQueryDto();
                queryDto.setEmail(edit.getEmail());
                if (getAll(queryDto).size() > 0) {
                    throw new RuntimeException("Email already belongs to another user");
                } else {
                    user.setEmail(edit.getEmail());
                }
            }
            if (edit.getCellphone() != null && edit.getCellphone() != "") {
                UserQueryDto queryDto = new UserQueryDto();
                queryDto.setCellphone(edit.getCellphone());
                if (getAll(queryDto).size() > 0) {
                    throw new RuntimeException("Cellphone already belongs to another user");
                } else {
                    user.setCellphone(edit.getCellphone());
                }
            }
            if (edit.getUserType() != null && edit.getUserType() != "") {
                user.setUserType(edit.getUserType().toUpperCase());
            }
            if (edit.getPassword() != null && edit.getPassword() != "") {
                user.setPasswordStatus(PasswordStatus.PRODUCTIVE);
                user.setPassword(encryptionService.encrypt(edit.getPassword(), secret).getBytes());
            }
            userRepository.save(user);
            return true;
        } else {
            throw new DoesNotExist();
        }

    }

    @Override
    public UserDto getUserById(String id) throws Exception {
        try {
            return entityToDto(userRepository.getById(id));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public PartnerDto getPartner(String user) {
        return null;
    }

    private boolean validatePassword(String enteredPassword, String storedPassword) {
        return encryptionService.encrypt(enteredPassword, secret).equals(storedPassword);
    }

    private UserDto entityToDto(UserEntity userEntity) {
        UserDto userDto = new UserDto();
        try {
            userDto.setId(userEntity.getId());
            userDto.setUsername(userEntity.getUsername());
            userDto.setPassword(new String(userEntity.getPassword(), "UTF-8"));
            userDto.setEmail(userEntity.getEmail());
            userDto.setCellphone(userEntity.getCellphone());
            userDto.setType(userEntity.getUserType());
            userDto.setStatus(userEntity.getStatus());
            userDto.setPasswordStatus(userEntity.getPasswordStatus());
            userDto.setValidFrom(userEntity.getValidFrom());
            userDto.setValidTo(userEntity.getValidTo());
            try {
                userDto.setPartner(partnerService.get(userEntity.getPartner()));
            } catch (PartnerNotFoundException e) {

            }
            userDto.setStatusReason(userEntity.getStatusReason());
            try {
                userDto.setPartnerDetails(partnerService.get(userEntity.getPartner()));
            } catch (PartnerNotFoundException exception) {
            }
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

    private Specification<UserEntity> findByCriteria(UserQueryDto userQuery) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (userQuery.getEmail() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("email"), userQuery.getEmail()));
            }
            if (userQuery.getUserType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("userType"), userQuery.getUserType()));
            }
            if (userQuery.getPartnerId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("partner"), userQuery.getPartnerId()));
            }
            if (userQuery.getCellphone() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cellphone"), userQuery.getCellphone()));
            }
            if (userQuery.getPasswordStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("passwordStatus"), userQuery.getPasswordStatus()));
            }
            if (userQuery.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), userQuery.getStatus()));
            }
            return predicate;
        };
    }

    private void notifyUser() {

    }

}
