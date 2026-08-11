package za.co.mawa.bes.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.configuration.context.TenantContext;

@Component
public class JwtUserDetailsService implements UserDetailsService {
    @Autowired
    EncryptionService encryptionService;
    @Value("${mawa.encryption.secret:${jwt.secret}}")
    private String encryptionSecret;
    @Autowired
    UserService userService;
    @Autowired
    UserAccessService userAccessService;
    @Value("${mawa.security.access-user-cache-seconds:5}")
    private long accessUserCacheSeconds;
    private final Map<String, AccessUserCacheEntry> accessUserCache = new ConcurrentHashMap<>();

    /**
     * Lightweight user snapshot for bearer-token authentication.
     *
     * Access-token validation does not need the user's decrypted password. The
     * normal UserDetailsService path intentionally performs password work for
     * interactive login, including BCrypt hashing. Reusing that path for every
     * API request made bearer authentication CPU-expensive and caused several
     * duplicate database reads per request.
     */
    public AccessTokenUser loadAccessTokenUser(String username) throws UsernameNotFoundException {
        String tenant = TenantContext.getCurrentTenant();
        String cacheKey = (tenant == null ? "" : tenant) + "|" + username;
        AccessUserCacheEntry cached = accessUserCache.get(cacheKey);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.user();
        }
        try {
            UserEntity user = userService.getUserEntityByName(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }

            userAccessService.validateUser(user);

            boolean accountNonLocked = !Status.LOCKED.equals(user.getStatus());
            UserDetails userDetails = new User(
                    user.getUsername(),
                    "",
                    true,
                    true,
                    true,
                    accountNonLocked,
                    new ArrayList<>()
            );

            AccessTokenUser accessTokenUser = new AccessTokenUser(
                    userDetails,
                    user.getId(),
                    user.getPartner(),
                    user.getPasswordChangedAt()
            );
            long ttl = Math.max(0, accessUserCacheSeconds);
            if (ttl > 0) {
                accessUserCache.put(cacheKey, new AccessUserCacheEntry(
                        accessTokenUser, now.plus(Duration.ofSeconds(ttl))));
            }
            return accessTokenUser;
        } catch (UsernameNotFoundException | DisabledException exception) {
            throw exception;
        } catch (SecurityException exception) {
            throw new DisabledException(exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new UsernameNotFoundException(
                    "Unable to load user with username: " + username,
                    exception
            );
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        boolean enabled = true;
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;
        try {
            UserEntity policyUser = userService.getUserEntityByName(username);
            if (policyUser != null) {
                userAccessService.validateUser(policyUser);
                if (Status.LOCKED.equals(policyUser.getStatus())) {
                    accountNonLocked = false;
                }

                byte[] passwordBytes = policyUser.getPassword();
                if (passwordBytes == null || passwordBytes.length == 0) {
                    throw new IllegalStateException(
                            "User password is not configured for username: " + username
                    );
                }

                String encryptedPassword = new String(passwordBytes, StandardCharsets.UTF_8);
                String decryptedPassword = encryptionService.decrypt(encryptedPassword, encryptionSecret);
                return new User(
                        policyUser.getUsername(),
                        new BCryptPasswordEncoder().encode(decryptedPassword),
                        enabled,
                        accountNonExpired,
                        credentialsNonExpired,
                        accountNonLocked,
                        new ArrayList<>()
                );
            }
            throw new UsernameNotFoundException("User not found with username: " + username);
        } catch (UsernameNotFoundException | DisabledException exception) {
            throw exception;
        } catch (SecurityException exception) {
            throw new DisabledException(exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new UsernameNotFoundException(
                    "Unable to load user with username: " + username,
                    exception
            );
        }
    }

    private record AccessUserCacheEntry(AccessTokenUser user, Instant expiresAt) {
    }

    public record AccessTokenUser(
            UserDetails userDetails,
            String userId,
            String partnerId,
            Date passwordChangedAt
    ) {
    }
}
