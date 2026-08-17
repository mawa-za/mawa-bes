package za.co.mawa.bes.service.v2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.service.UserService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Establishes the trusted, non-interactive identity used by background work.
 *
 * <p>BGUSER is deliberately separate from the protected {@code system} user.
 * It has no application authorities and must not be used for interactive login.
 * Flyway creates the tenant BGUSER row so audit fields can resolve to a stable
 * user id where required. A synthetic fallback is kept so a background worker
 * fails safely during a rolling deployment where application code reaches a
 * tenant before its migration has completed.</p>
 */
@Slf4j
@Service
public class BackgroundExecutionContextService {

    public static final String BACKGROUND_USERNAME = "BGUSER";
    private static final Set<String> MISSING_BGUSER_TENANTS = ConcurrentHashMap.newKeySet();

    private final UserService userService;

    public BackgroundExecutionContextService(UserService userService) {
        this.userService = userService;
    }

    public void establish() {
        UserEntity backgroundUser = resolveBackgroundUser();

        UserDetails principal = User.withUsername(BACKGROUND_USERNAME)
                .password("")
                .authorities(Collections.emptyList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        UserContext.setCurrentUser(BACKGROUND_USERNAME);
        UserContext.setCurrentUserId(
                backgroundUser == null || backgroundUser.getId() == null || backgroundUser.getId().isBlank()
                        ? BACKGROUND_USERNAME
                        : backgroundUser.getId()
        );
        UserContext.setCurrentUserPartner(backgroundUser == null ? null : backgroundUser.getPartner());
        UserContext.setPlatformSession(false);
        UserContext.setBackgroundSession(true);
        UserContext.setAccountType(backgroundUser == null ? "STANDARD" : backgroundUser.getAccountType());
        UserContext.setAccessScope(backgroundUser == null ? "BACKGROUND" : backgroundUser.getAccessScope());
        UserContext.setTestUser(false);
        UserContext.setProtectedUser(true);
        UserContext.setExternalTransactionsBlocked(false);
    }

    public void clear() {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    private UserEntity resolveBackgroundUser() {
        try {
            UserEntity user = userService.getUserEntityByName(BACKGROUND_USERNAME);
            if (user != null) {
                return user;
            }
        } catch (Exception ex) {
            log.warn("Unable to resolve BGUSER for tenant {}: {}",
                    TenantContext.getCurrentTenant(), ex.getMessage());
        }

        String tenant = TenantContext.getCurrentTenant();
        String warningKey = tenant == null || tenant.isBlank() ? "unknown" : tenant;
        if (MISSING_BGUSER_TENANTS.add(warningKey)) {
            log.warn("Tenant {} does not have BGUSER; using a synthetic background identity until the tenant migration is applied",
                    warningKey);
        }
        return null;
    }
}
