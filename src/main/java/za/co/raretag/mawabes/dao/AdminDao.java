package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.JwtRequest;

public interface AdminDao {
    String authenticate(JwtRequest authenticationRequest);
}
