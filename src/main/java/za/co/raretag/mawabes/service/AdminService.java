package za.co.raretag.mawabes.service;

import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.AdminDao;
import za.co.raretag.mawabes.dto.JwtRequest;
@Service
public class AdminService implements AdminDao {
    @Override
    public String authenticate(JwtRequest authenticationRequest) {
        return null;
    }
}
