package za.co.mawa.bes.service;

import java.util.ArrayList;
import java.util.Date;

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
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.utils.Status;

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

            return new AccessTokenUser(
                    userDetails,
                    user.getId(),
                    user.getPartner(),
                    user.getPasswordChangedAt()
            );
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
            UserDto userDto = userService.getUserByName(username);
            if (userDto != null) {
                za.co.mawa.bes.entity.UserEntity policyUser = userService.getUserEntityByName(username);
                userAccessService.validateUser(policyUser);
                if (Status.LOCKED.equals(userDto.getStatus())) {
                    accountNonLocked = false;
                }

                String encryptedPassword = userDto.getPassword();
                if (encryptedPassword == null || encryptedPassword.isBlank()) {
                    throw new IllegalStateException(
                            "User password is not configured for username: " + username
                    );
                }

                String decryptedPassword = encryptionService.decrypt(encryptedPassword, encryptionSecret);
                return new User(
                        userDto.getUsername(),
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

    public record AccessTokenUser(
            UserDetails userDetails,
            String userId,
            String partnerId,
            Date passwordChangedAt
    ) {
    }
}
