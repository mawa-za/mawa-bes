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
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.exception.UserLockedException;
import za.co.mawa.bes.utils.Status;

@Component
public class JwtUserDetailsService implements UserDetailsService {
    @Autowired
    EncryptionService encryptionService;
    @Value("${jwt.secret}")
    private String secret;
    @Autowired
    UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        boolean enabled = true;
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;
        try {
            UserDto userDto = userService.getUserByName(username);
            if (userDto != null) {
                if (userDto.getStatus().equals(Status.LOCKED)){
                    accountNonLocked = false;
                }
                String decryptedPassword = encryptionService.decrypt(userDto.getPassword().toString(), secret);
                User user = new User(userDto.getUsername(), new BCryptPasswordEncoder().encode(decryptedPassword),
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, new ArrayList<>());
                return user;
            } else {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}