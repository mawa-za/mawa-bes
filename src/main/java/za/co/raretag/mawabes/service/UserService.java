package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.raretag.mawabes.object.token.JwtRequest;
import za.co.raretag.mawabes.object.user.UserDAO;
import za.co.raretag.mawabes.object.user.UserDTO;
import za.co.raretag.mawabes.repository.UserRepository;

import javax.persistence.Access;

@Component
public class UserService implements UserDAO {
    @Autowired
    UserRepository userRepository;
    @Override
    public boolean authenticate(JwtRequest jwtRequest) {
        userRepository.getById(jwtRequest.getUsername());
        return true;
    }
}
