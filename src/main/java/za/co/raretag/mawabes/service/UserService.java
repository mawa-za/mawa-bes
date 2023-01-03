package za.co.raretag.mawabes.service;

import za.co.raretag.mawabes.object.user.UserDAO;
import za.co.raretag.mawabes.object.user.UserDTO;

public class UserService implements UserDAO {
    @Override
    public boolean authenticate(UserDTO userDTO) {
        return false;
    }
}
