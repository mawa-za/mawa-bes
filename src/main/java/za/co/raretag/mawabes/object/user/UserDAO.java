package za.co.raretag.mawabes.object.user;

import za.co.raretag.mawabes.object.authentication.AuthenticationDTO;

public interface UserDAO {
    boolean authenticate(AuthenticationDTO authenticationDTO);
}
