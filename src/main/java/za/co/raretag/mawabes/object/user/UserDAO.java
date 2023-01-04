package za.co.raretag.mawabes.object.user;

import za.co.raretag.mawabes.object.token.JwtRequest;

public interface UserDAO {
    boolean authenticate(JwtRequest jwtRequest);
}
