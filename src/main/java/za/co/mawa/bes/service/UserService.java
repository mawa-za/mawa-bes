package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.UserDao;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.user.*;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.UserExistException;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.utils.*;

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
    EmailService emailService;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserAccessService userAccessService;
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    SimpleKeyGenerator keyGenerator;
    @Autowired
    PartnerService partnerService;
    @Autowired
    SettingService settingService;
    private String encryptionSecret;
    public static final String SYSTEM_USER = "system";
    public static final String DEFAULT_SYSTEM_PASSWORD = "system";

    @Value("${mawa.encryption.secret:${jwt.secret}}")
    public void setEncryptionSecret(String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
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
        userEntity.setUserType(userCreateDto.getUserType() == null ? UserType.ADMIN : userCreateDto.getUserType().toUpperCase());
        userEntity.setAccountType(userCreateDto.getAccountType() == null ? "STANDARD" : userCreateDto.getAccountType().toUpperCase());
        userEntity.setTestUser(Boolean.TRUE.equals(userCreateDto.getTestUser()));
        // Protection and all-access are derived from protected role assignment.
        userEntity.setProtectedUser(false);
        userEntity.setSystemManaged(false);
        userEntity.setAccessScope("STANDARD");
        userEntity.setEnvironmentScope(userCreateDto.getEnvironmentScope());
        userEntity.setExternalTransactionsBlocked(Boolean.TRUE.equals(userCreateDto.getExternalTransactionsBlocked()));
        userEntity.setExpiresAt(userCreateDto.getExpiresAt());
        if ("SUPPORT_VERIFICATION".equalsIgnoreCase(userEntity.getAccountType()) && userEntity.getExpiresAt() == null) {
            throw new IllegalArgumentException("Temporary support access requires an expiry date");
        }
        userEntity.setProtectedReason(userCreateDto.getProtectedReason());
        userEntity.setMfaRequired(Boolean.TRUE.equals(userCreateDto.getMfaRequired()));
        if (Boolean.TRUE.equals(userEntity.getProtectedUser())) {
            userEntity.setProtectedAt(new Date());
            userEntity.setProtectedBy(UserContext.getCurrentUser());
        }
        if (Boolean.TRUE.equals(userEntity.getTestUser()) && userEntity.getEnvironmentScope() == null) {
            userEntity.setEnvironmentScope("DEV,ALPHA,BETA");
            userEntity.setExternalTransactionsBlocked(true);
        }
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setPasswordStatus(PasswordStatus.INITIAL);
        userEntity.setValidFrom(new Date());
        userEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
        if (userCreateDto.getPassword() == null) {
            userCreateDto.setPassword(keyGenerator.generatePassword());
        }
        userEntity.setPassword(encryptionService.encrypt(userCreateDto.getPassword(), encryptionSecret).getBytes());
        userEntity.setPasswordChangedAt(new Date());
        UserDto userDto = entityToDto(userRepository.save(userEntity));
        EmailDto emailDto = new EmailDto();
        emailDto.setTo(userEntity.getEmail());
        emailDto.setSubject("New user");
        emailDto.setTemplate("new-user");
        List<PropertyDto> props = new ArrayList<>();
        props.add(new PropertyDto(HtmlTemplateVariableKey.USER_NAME, userCreateDto.getUsername()));
        props.add(new PropertyDto(HtmlTemplateVariableKey.USER_PASSWORD, userCreateDto.getPassword()));
        props.add(new PropertyDto(HtmlTemplateVariableKey.TENANT_URL, buildTenantURL()));

        emailDto.setProperties(props);
        try {
            emailService.send(emailDto);
        } catch (Exception ex) {

        }
        return userDto;
    }

    public String buildTenantURL() {

        String domain = settingService.getSetting("ACCESS-URL","TENANT");
        if(domain == null){
            domain = TenantContext.getCurrentTenantURL();
        }
        return domain.startsWith("http://") || domain.startsWith("https://")
                ? domain
                : "https://" + domain;
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
            userEntity.setPassword(encryptionService.encrypt(userUpdateDto.getPassword(), encryptionSecret).getBytes());
            userEntity.setPasswordChangedAt(new Date());
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
                    userCreateDto.setAccountType("STANDARD");
                    userCreateDto.setProtectedUser(true);
                    userCreateDto.setSystemManaged(true);
                    userCreateDto.setAccessScope("TENANT_ALL");
                    userCreateDto.setProtectedReason("Required for Admin Console tenant handoff");
                    userCreateDto.setMfaRequired(true);
                    userDto = create(userCreateDto);
                    UserRoleDto userRoleDto = new UserRoleDto();
                    userRoleDto.setUser(userDto.getId());
                    userRoleDto.setRole("SYSTEM");
                    addRole(userRoleDto);
                    UserEntity systemUser = userRepository.getById(userDto.getId());
                    systemUser.setSystemManaged(true);
                    systemUser.setProtectedUser(true);
                    systemUser.setAccessScope("TENANT_ALL");
                    systemUser.setProtectedReason("Required for Admin Console tenant handoff");
                    systemUser.setMfaRequired(true);
                    if (systemUser.getProtectedAt() == null) systemUser.setProtectedAt(new Date());
                    userRepository.save(systemUser);
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

    public String getCurrentUserPartnerId() {
        try {

            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserEntity user = userRepository.getByName(userDetails.getUsername());

            if (user != null) {

                return String.valueOf(user.getPartner());
            } else {
                return null;
            }
        } catch (Exception e) {

            return null;
        }
    }

    public String getCurrentUserId() {
        try {

            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserEntity user = userRepository.getByName(userDetails.getUsername());

            if (user != null) {

                return String.valueOf(user.getId());
            } else {
                return null;
            }
        } catch (Exception e) {

            return null;
        }
    }

    @Override
    public void addRole(UserRoleDto userRoleDto) throws Exception {
        try {
            za.co.mawa.bes.entity.RoleEntity requestedRole = roleRepository.findById(userRoleDto.getRole())
                    .orElseThrow(() -> new IllegalArgumentException("Role does not exist: " + userRoleDto.getRole()));
            UserEntity targetUser = userRepository.getById(userRoleDto.getUser());
            boolean systemBootstrap = targetUser != null && SYSTEM_USER.equalsIgnoreCase(targetUser.getUsername())
                    && "SYSTEM".equalsIgnoreCase(requestedRole.getId());
            if ((Boolean.TRUE.equals(requestedRole.getProtectedRole())
                    || Boolean.TRUE.equals(requestedRole.getSystemRole())
                    || Boolean.TRUE.equals(requestedRole.getAccessAllWorkcentres()))
                    && !systemBootstrap && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can assign protected or all-access roles");
            }
            UserRolePKEntity userRolePKEntity = new UserRolePKEntity();
            userRolePKEntity.setUser(userRoleDto.getUser());
            userRolePKEntity.setRole(userRoleDto.getRole());
            UserRoleEntity userRoleEntity = new UserRoleEntity();
            userRoleEntity.setUserRolePKEntity(userRolePKEntity);
            userRoleEntity.setValidFrom(new Date());
            userRoleEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            userRoleRepository.save(userRoleEntity);
            roleRepository.findById(userRoleDto.getRole()).ifPresent(role -> {
                if (Boolean.TRUE.equals(role.getAccessAllWorkcentres())) {
                    UserEntity protectedUser = userRepository.getById(userRoleDto.getUser());
                    protectedUser.setProtectedUser(true);
                    protectedUser.setAccessScope("TENANT_ALL");
                    protectedUser.setMfaRequired(true);
                    if (protectedUser.getProtectedAt() == null) protectedUser.setProtectedAt(new Date());
                    if (protectedUser.getProtectedReason() == null || protectedUser.getProtectedReason().isBlank()) {
                        protectedUser.setProtectedReason("Assigned protected role " + role.getId());
                    }
                    protectedUser.setProtectedBy(UserContext.getCurrentUser());
                    userRepository.save(protectedUser);
                }
            });
        } catch (IllegalArgumentException | IllegalStateException | SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new Exception("Unable to assign role: " + exception.getMessage(), exception);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void replaceRoles(String userId, java.util.List<String> requestedRoles) throws Exception {
        java.util.Set<String> requested = requestedRoles == null
                ? java.util.Set.of()
                : requestedRoles.stream().filter(java.util.Objects::nonNull)
                    .map(String::trim).filter(v -> !v.isEmpty())
                    .map(v -> v.toUpperCase(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        for (String roleId : requested) {
            if (!roleRepository.existsById(roleId)) {
                throw new IllegalArgumentException("Role does not exist: " + roleId);
            }
        }

        UserEntity targetUser = userRepository.getById(userId);
        java.util.List<UserRoleEntity> existingAssignments = userRoleRepository.findUserRoles(userId);
        java.util.Set<String> existingIds = existingAssignments.stream()
                .map(x -> x.getUserRolePKEntity().getRole().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.Set<String> changed = new java.util.LinkedHashSet<>(existingIds);
        changed.addAll(requested);
        java.util.Set<String> unchanged = new java.util.LinkedHashSet<>(existingIds);
        unchanged.retainAll(requested);
        changed.removeAll(unchanged);
        boolean protectedRoleChanged = changed.stream().map(roleRepository::findById)
                .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .anyMatch(role -> Boolean.TRUE.equals(role.getProtectedRole())
                        || Boolean.TRUE.equals(role.getSystemRole())
                        || Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
        if (protectedRoleChanged && !userAccessService.isProtectedAdministrator()) {
            throw new SecurityException("Only a protected tenant administrator can add or remove protected/all-access roles");
        }
        boolean systemUser = targetUser != null && SYSTEM_USER.equalsIgnoreCase(targetUser.getUsername());
        if (systemUser && existingIds.contains("SYSTEM") && !requested.contains("SYSTEM")) {
            throw new IllegalStateException("PROTECTED_USER: The bootstrap SYSTEM role cannot be removed");
        }

        for (UserRoleEntity assignment : existingAssignments) {
            String roleId = assignment.getUserRolePKEntity().getRole();
            if (!requested.contains(roleId.toUpperCase(java.util.Locale.ROOT))) {
                deleteRole(assignment.getUserRolePKEntity());
            }
        }
        java.util.Set<String> remainingIds = userRoleRepository.findUserRoles(userId).stream()
                .map(x -> x.getUserRolePKEntity().getRole().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        for (String roleId : requested) {
            if (!remainingIds.contains(roleId)) {
                UserRoleDto dto = new UserRoleDto();
                dto.setUser(userId);
                dto.setRole(roleId);
                addRole(dto);
            }
        }
        boolean grantsTenantAll = requested.stream()
                .map(roleRepository::findById).filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .anyMatch(role -> Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
        UserEntity reconciledUser = userRepository.getById(userId);
        if (grantsTenantAll) {
            reconciledUser.setProtectedUser(true);
            reconciledUser.setAccessScope("TENANT_ALL");
            reconciledUser.setMfaRequired(true);
            if (reconciledUser.getProtectedAt() == null) reconciledUser.setProtectedAt(new Date());
            if (reconciledUser.getProtectedReason() == null || reconciledUser.getProtectedReason().isBlank()) {
                reconciledUser.setProtectedReason("Assigned access-all role through Role Maintenance");
            }
            reconciledUser.setProtectedBy(UserContext.getCurrentUser());
        } else if (!Boolean.TRUE.equals(reconciledUser.getSystemManaged())) {
            reconciledUser.setProtectedUser(false);
            reconciledUser.setAccessScope("STANDARD");
        }
        userRepository.save(reconciledUser);
    }

    @Override
    public boolean lockuser(String id, String statusReason) throws Exception {
        UserEntity user = userRepository.getById(id);
        if ((Boolean.TRUE.equals(user.getProtectedUser()) || Boolean.TRUE.equals(user.getSystemManaged()))
                && !userAccessService.isProtectedAdministrator()) {
            throw new SecurityException("Only a protected tenant administrator can lock a protected user");
        }
        boolean accessAllUser = getRoles(id).stream().map(roleRepository::findById)
                .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .anyMatch(role -> Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
        if (accessAllUser && userRoleRepository.countActiveAccessAllUsers() <= 1) {
            throw new IllegalStateException("LAST_TENANT_SUPER_ADMIN: At least one active protected tenant administrator must remain");
        }
        user.setStatus(UserStatus.LOCKED);
        user.setStatusReason(statusReason);
        user.setDisabledAt(new Date());
        user.setDisabledBy(UserContext.getCurrentUser());
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean unlockuser(String id) throws Exception {
        try {
            UserEntity user = userRepository.getById(id);
            if ((Boolean.TRUE.equals(user.getProtectedUser()) || Boolean.TRUE.equals(user.getSystemManaged()))
                    && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can unlock a protected user");
            }
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
        UserEntity user = userRepository.getById(entityPk.getUser());
        za.co.mawa.bes.entity.RoleEntity role = roleRepository.findById(entityPk.getRole()).orElse(null);
        boolean protectedRole = role != null && (Boolean.TRUE.equals(role.getProtectedRole())
                || Boolean.TRUE.equals(role.getSystemRole()) || Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
        if (protectedRole && !userAccessService.isProtectedAdministrator()) {
            throw new SecurityException("Only a protected tenant administrator can remove protected/all-access roles");
        }
        if (Boolean.TRUE.equals(user.getSystemManaged()) && role != null && Boolean.TRUE.equals(role.getAccessAllWorkcentres())) {
            throw new IllegalStateException("PROTECTED_USER: The bootstrap SYSTEM role cannot be removed");
        }
        if (role != null && Boolean.TRUE.equals(role.getAccessAllWorkcentres()) && Boolean.TRUE.equals(user.getProtectedUser())) {
            boolean hasAnotherAccessAllRole = getRoles(entityPk.getUser()).stream()
                    .filter(roleId -> !roleId.equalsIgnoreCase(entityPk.getRole()))
                    .map(roleRepository::findById).filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                    .anyMatch(other -> Boolean.TRUE.equals(other.getAccessAllWorkcentres()));
            if (!hasAnotherAccessAllRole && userRoleRepository.countActiveAccessAllUsers() <= 1) {
                throw new IllegalStateException("LAST_TENANT_SUPER_ADMIN: At least one active protected tenant administrator must remain");
            }
        }
        userRoleRepository.deleteById(entityPk);
        boolean stillAccessAll = getRoles(entityPk.getUser()).stream().map(roleRepository::findById)
                .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .anyMatch(other -> Boolean.TRUE.equals(other.getAccessAllWorkcentres()));
        if (!stillAccessAll && !Boolean.TRUE.equals(user.getSystemManaged())) {
            user.setProtectedUser(false);
            user.setAccessScope("STANDARD");
            userRepository.save(user);
        }
        return true;
    }

    @Override
    public String resetUser(String id) throws Exception {
        try {
            String password = keyGenerator.generatePassword();
            UserEntity userEntity = userRepository.getById(id);
            if ((Boolean.TRUE.equals(userEntity.getProtectedUser()) || Boolean.TRUE.equals(userEntity.getSystemManaged()))
                    && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can reset a protected user");
            }
//            PartnerDto partnerDto = partnerService.get(userEntity.getPartner());
            userEntity.setPassword(encryptionService.encrypt(password, encryptionSecret).getBytes());
            userEntity.setPasswordChangedAt(new Date());
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
        UserEntity user = userRepository.getById(id);
        if (Boolean.TRUE.equals(user.getProtectedUser()) || Boolean.TRUE.equals(user.getSystemManaged())) {
            throw new IllegalStateException("PROTECTED_USER: This protected system administrator cannot be deleted");
        }
        userRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean editUser(String id, UserEditDto edit) throws Exception {
        UserEntity user = userRepository.getById(id);

        if (user != null) {
            if ((Boolean.TRUE.equals(user.getProtectedUser()) || Boolean.TRUE.equals(user.getSystemManaged()))
                    && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can edit a protected user");
            }
            if (edit.getEmail() != null && edit.getEmail() != "") {
                UserQueryDto queryDto = new UserQueryDto();
                queryDto.setEmail(edit.getEmail());
                if (getAll(queryDto).size() > 0) {
                    throw new RuntimeException("Email already belongs to another user");
                } else {
                    user.setEmail(edit.getEmail());
                }
            }
            ///

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
            if (edit.getStatus() != null && !edit.getStatus().isBlank()) {
                boolean deactivating = UserStatus.ACTIVE.equalsIgnoreCase(user.getStatus())
                        && !UserStatus.ACTIVE.equalsIgnoreCase(edit.getStatus());
                boolean accessAllUser = getRoles(id).stream().map(roleRepository::findById)
                        .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                        .anyMatch(role -> Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
                if (deactivating && accessAllUser && userRoleRepository.countActiveAccessAllUsers() <= 1) {
                    throw new IllegalStateException("LAST_TENANT_SUPER_ADMIN: At least one active protected tenant administrator must remain");
                }
                user.setStatus(edit.getStatus().toUpperCase());
            }
            if (edit.getStatusReason() != null) user.setStatusReason(edit.getStatusReason());
            if (edit.getAccountType() != null && !edit.getAccountType().isBlank()) user.setAccountType(edit.getAccountType().toUpperCase());
            if (edit.getTestUser() != null) user.setTestUser(edit.getTestUser());
            // Protected status and TENANT_ALL are derived only from access-all roles in Role Maintenance.
            if (edit.getEnvironmentScope() != null) user.setEnvironmentScope(edit.getEnvironmentScope());
            if (edit.getExternalTransactionsBlocked() != null) user.setExternalTransactionsBlocked(edit.getExternalTransactionsBlocked());
            if (edit.getExpiresAt() != null) user.setExpiresAt(edit.getExpiresAt());
            if ("SUPPORT_VERIFICATION".equalsIgnoreCase(user.getAccountType()) && user.getExpiresAt() == null) {
                throw new IllegalArgumentException("Temporary support access requires an expiry date");
            }
            if (edit.getProtectedReason() != null) user.setProtectedReason(edit.getProtectedReason());
            if (edit.getMfaRequired() != null) user.setMfaRequired(edit.getMfaRequired());
            if (Boolean.TRUE.equals(user.getTestUser()) && user.getEnvironmentScope() == null) {
                user.setEnvironmentScope("DEV,ALPHA,BETA");
                user.setExternalTransactionsBlocked(true);
            }
            if (edit.getPassword() != null && edit.getPassword() != "") {
                user.setPasswordStatus(PasswordStatus.PRODUCTIVE);
                user.setPassword(encryptionService.encrypt(edit.getPassword(), encryptionSecret).getBytes());
                user.setPasswordChangedAt(new Date());
            }
            userRepository.save(user);
            return true;
        } else {
            throw new DoesNotExist();
        }

    }

    @Override
    public PartnerDto getPartner(String user) {
        return null;
    }

    private boolean validatePassword(String enteredPassword, String storedPassword) {
        return encryptionService.encrypt(enteredPassword, encryptionSecret).equals(storedPassword);
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
            userDto.setAccountType(userEntity.getAccountType());
            userDto.setTestUser(userEntity.getTestUser());
            userDto.setProtectedUser(userEntity.getProtectedUser());
            userDto.setSystemManaged(userEntity.getSystemManaged());
            userDto.setAccessScope(userEntity.getAccessScope());
            userDto.setEnvironmentScope(userEntity.getEnvironmentScope());
            userDto.setExternalTransactionsBlocked(userEntity.getExternalTransactionsBlocked());
            userDto.setExpiresAt(userEntity.getExpiresAt());
            userDto.setProtectedReason(userEntity.getProtectedReason());
            userDto.setProtectedAt(userEntity.getProtectedAt());
            userDto.setProtectedBy(userEntity.getProtectedBy());
            userDto.setDisabledAt(userEntity.getDisabledAt());
            userDto.setDisabledBy(userEntity.getDisabledBy());
            userDto.setMfaRequired(userEntity.getMfaRequired());
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
    @Override
    public UserDto getUserById(String id) throws Exception {
        try {
            return entityToDto(userRepository.getById(id));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public UserEntity getUserEntityByName(String username) {
        return userRepository.getByName(username);
    }

    public UserDto getUserByEmail(String email) throws Exception {
        try {
            UserEntity userEntity = userRepository.getByEmail(email);
            if (userEntity == null) {
                throw new DoesNotExist("User with the given email does not exist");
            }
            return entityToDto(userEntity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public UserDto getUserByCellphone(String cellphone) throws Exception {
        try {
            UserEntity userEntity = userRepository.getByCellphone(cellphone);
            if (userEntity == null) {
                throw new DoesNotExist("User with the given cellphone number does not exist");
            }
            return entityToDto(userEntity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public UserDto getUserByPartnerId(String partnerId) throws Exception {
        try {
            UserEntity userEntity = userRepository.getByPartner(partnerId);
            if (userEntity == null) {
                throw new DoesNotExist("User with the given partner ID does not exist");
            }
            return entityToDto(userEntity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}

