package za.co.mawa.bes.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
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
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
