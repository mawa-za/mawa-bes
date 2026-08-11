package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.access.UserAccessProfileDto;
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.access.UserAccessAuditEntity;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.RoleWorkcenterRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.repository.access.UserAccessAuditRepository;
import za.co.mawa.bes.utils.UserStatus;

import java.util.*;

@Service
public class UserAccessService {
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RoleWorkcenterRepository roleWorkcenterRepository;
    @Autowired private UserAccessAuditRepository auditRepository;
    @Value("${spring.profiles.active:local}") private String environment;

    public UserAccessProfileDto profile() {
        if (UserContext.isPlatformSession()) {
            String handoffRole = defaultText(UserContext.getHandoffRoleId(), "SUPPORT_VERIFICATION");
            RoleEntity role = roleRepository.findById(handoffRole).orElse(null);
            boolean allWorkcentres = "PLATFORM_ALL".equalsIgnoreCase(UserContext.getAccessScope())
                    || (role != null && Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
            return UserAccessProfileDto.builder()
                    .userId(UserContext.getCurrentUserId()).username(UserContext.getPlatformUsername())
                    .displayName(UserContext.getPlatformDisplayName()).email(UserContext.getPlatformEmail())
                    .accountType(defaultText(UserContext.getAccountType(), "STANDARD"))
                    .testUser(UserContext.isTestUser()).protectedUser(UserContext.isProtectedUser()).systemManaged(true)
                    .accessScope(defaultText(UserContext.getAccessScope(), "STANDARD"))
                    .externalTransactionsBlocked(UserContext.isExternalTransactionsBlocked())
                    .expiresAt(UserContext.getAccessExpiresAt()).mfaRequired(true).platformSession(true)
                    .platformUserId(UserContext.getPlatformUserId()).handoffId(UserContext.getHandoffId())
                    .accessReason(UserContext.getAccessReason()).ticketReference(UserContext.getTicketReference())
                    .tenantId(TenantContext.getCurrentTenant()).roles(List.of(handoffRole))
                    .allWorkcentres(allWorkcentres).build();
        }
        String username = currentUsername();
        UserEntity user = StringUtils.hasText(username) ? userRepository.getByName(username) : null;
        if (user == null) throw new SecurityException("User is not authenticated");
        validateUser(user);
        List<RoleEntity> activeRoles = activeRoles(user.getId());
        List<String> roles = activeRoles.stream().map(RoleEntity::getId).toList();
        boolean all = activeRoles.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getAccessAllWorkcentres()));
        return UserAccessProfileDto.builder().userId(user.getId()).username(user.getUsername()).email(user.getEmail())
                .accountType(defaultText(user.getAccountType(), "STANDARD")).testUser(Boolean.TRUE.equals(user.getTestUser()))
                .protectedUser(Boolean.TRUE.equals(user.getProtectedUser())).systemManaged(Boolean.TRUE.equals(user.getSystemManaged()))
                .accessScope(defaultText(user.getAccessScope(), "STANDARD")).environmentScope(user.getEnvironmentScope())
                .externalTransactionsBlocked(Boolean.TRUE.equals(user.getExternalTransactionsBlocked())).expiresAt(user.getExpiresAt())
                .mfaRequired(Boolean.TRUE.equals(user.getMfaRequired())).platformSession(false).tenantId(TenantContext.getCurrentTenant())
                .roles(roles).allWorkcentres(all).build();
    }

    public void validateCurrentSession() {
        if (UserContext.isPlatformSession()) {
            if (UserContext.getAccessExpiresAt() != null && UserContext.getAccessExpiresAt().before(new Date())) {
                throw new SecurityException("Platform access has expired");
            }
            return;
        }
        if (!hasInteractiveSession()) {
            return;
        }
        String username = currentUsername();
        if (StringUtils.hasText(username)) {
            validateUser(userRepository.getByName(username));
        }
    }

    public void validateUser(UserEntity user) {
        if(user==null) throw new SecurityException("User does not exist");
        if(!UserStatus.ACTIVE.equalsIgnoreCase(user.getStatus())) throw new SecurityException("User is not active");
        if(user.getExpiresAt()!=null && user.getExpiresAt().before(new Date())) throw new SecurityException("User access has expired");
        if(Boolean.TRUE.equals(user.getTestUser()) && !environmentAllowed(user.getEnvironmentScope()))
            throw new SecurityException("Testing user is not permitted in the " + environment + " environment");
    }

    public boolean environmentAllowed(String scope) {
        if(!StringUtils.hasText(scope)) return true;
        String active=environment==null?"LOCAL":environment.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(scope.split("[,;]")).map(String::trim).map(v->v.toUpperCase(Locale.ROOT))
                .anyMatch(v->v.equals("ALL")||v.equals(active));
    }

    public boolean isProtectedAdministrator() {
        if (UserContext.isPlatformSession()) {
            return "PLATFORM_ALL".equalsIgnoreCase(UserContext.getAccessScope());
        }
        String username = currentUsername();
        UserEntity user = StringUtils.hasText(username) ? userRepository.getByName(username) : null;
        if (user == null) return false;
        return activeRoles(user.getId()).stream()
                .anyMatch(role -> Boolean.TRUE.equals(role.getAccessAllWorkcentres()));
    }

    public boolean hasWorkcentreAccess(String workcentreId, String selectedRoleId) {
        if (!StringUtils.hasText(workcentreId)) return false;

        String normalizedWorkcentre = workcentreId.trim().toLowerCase(Locale.ROOT);
        UserAccessProfileDto accessProfile = profile();
        if (UserContext.isPlatformSession()
                && Boolean.TRUE.equals(accessProfile.getAllWorkcentres())) {
            return true;
        }
        List<String> accessibleRoles = accessProfile.getRoles() == null
                ? List.of()
                : accessProfile.getRoles();

        List<String> rolesToCheck;
        if (StringUtils.hasText(selectedRoleId)) {
            String selectedRole = selectedRoleId.trim();
            String assignedRole = accessibleRoles.stream()
                    .filter(roleId -> roleId.equalsIgnoreCase(selectedRole))
                    .findFirst()
                    .orElse(null);
            if (assignedRole == null) return false;
            rolesToCheck = List.of(assignedRole);
        } else {
            if (Boolean.TRUE.equals(accessProfile.getAllWorkcentres())) return true;
            rolesToCheck = accessibleRoles;
        }

        for (String roleId : rolesToCheck) {
            RoleEntity role = roleRepository.findById(roleId).orElse(null);
            if (role == null) continue;
            if (Boolean.TRUE.equals(role.getAccessAllWorkcentres())) return true;

            za.co.mawa.bes.entity.RoleWorkcenterPKEntity key =
                    new za.co.mawa.bes.entity.RoleWorkcenterPKEntity();
            key.setRole(roleId);
            key.setWorkcenter(normalizedWorkcentre);
            if (roleWorkcenterRepository.existsById(key)) return true;
        }
        return false;
    }

    public void assertWorkcentreAccess(String workcentreId, String selectedRoleId) {
        if (hasWorkcentreAccess(workcentreId, selectedRoleId)) return;
        throw new SecurityException(
                "You do not have access to configure " + workcentreId.replace('-', ' ')
        );
    }

    public boolean externalTransactionsBlocked() {
        UserAccessProfileDto p=profile();
        boolean prod="PROD".equalsIgnoreCase(environment)||"PRODUCTION".equalsIgnoreCase(environment);
        return Boolean.TRUE.equals(p.getExternalTransactionsBlocked()) || (prod && Boolean.TRUE.equals(p.getTestUser()));
    }

    public boolean hasInteractiveSession() {
        if (UserContext.isPlatformSession()) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equalsIgnoreCase(authentication.getName());
    }

    public boolean externalTransactionsBlockedForInteractiveSession() {
        return hasInteractiveSession() && externalTransactionsBlocked();
    }

    public void assertExternalTransactionAllowed(String operation) {
        if (!externalTransactionsBlockedForInteractiveSession()) return;
        audit("EXTERNAL_TRANSACTION_BLOCKED", "OPERATION", operation,
                "Test-user access policy", "External transaction was not queued");
        throw new SecurityException("Testing users cannot execute " + operation + " in the current environment");
    }

    public void audit(String action,String targetType,String targetId,String reason,String details){
        UserAccessProfileDto p;
        try{p=profile();}catch(Exception e){p=null;}
        auditRepository.save(UserAccessAuditEntity.builder().userId(p==null?UserContext.getCurrentUserId():p.getUserId())
                .username(p==null?currentUsername():p.getUsername()).action(action).targetType(targetType).targetId(targetId)
                .reason(reason).details(details).createdAt(new Date()).build());
    }


    private List<String> activeRoleIds(String userId) {
        return activeRoles(userId).stream().map(RoleEntity::getId).toList();
    }

    private List<RoleEntity> activeRoles(String userId) {
        Date now = new Date();
        List<String> assignedRoleIds = userRoleRepository.findUserRoles(userId).stream()
                .filter(assignment -> assignment.getValidFrom() == null || !assignment.getValidFrom().after(now))
                .filter(assignment -> assignment.getValidTo() == null || !assignment.getValidTo().before(now))
                .map(assignment -> assignment.getUserRolePKEntity().getRole())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (assignedRoleIds.isEmpty()) return List.of();
        return roleRepository.findAllById(assignedRoleIds).stream()
                .filter(role -> role.getValidFrom() == null || !role.getValidFrom().after(now))
                .filter(role -> role.getValidTo() == null || !role.getValidTo().before(now))
                .toList();
    }

    private String currentUsername(){ Authentication a=SecurityContextHolder.getContext().getAuthentication(); return a==null?UserContext.getCurrentUser():a.getName(); }
    private String defaultText(String value,String fallback){return StringUtils.hasText(value)?value:fallback;}
}
