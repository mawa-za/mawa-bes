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
        UserDto userDto = null;
        try {
            userDto = userService.getUserByName(username);
            if (userDto != null) {
                String decryptedPassword = encryptionService.decrypt(userDto.getPassword().toString(), secret);
                String bcrypt = new BCryptPasswordEncoder().encode(decryptedPassword);
                return new User(userDto.getUsername(), new BCryptPasswordEncoder().encode(decryptedPassword), new ArrayList<>());
            } else {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}