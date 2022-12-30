package za.co.raretag.mawabes.object.user;

import za.co.raretag.mawabes.object.authentication.AuthenticationDTO;

public class UserDataAccessService implements  UserDAO{
    @Override
    public boolean authenticate(AuthenticationDTO authenticationDTO) {
        return true;
    }
}
