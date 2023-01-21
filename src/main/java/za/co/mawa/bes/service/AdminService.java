package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.AdminDao;
import za.co.mawa.bes.dto.JwtRequest;
@Service
public class AdminService implements AdminDao {
    @Override
    public String authenticate(JwtRequest authenticationRequest) {
        return null;
    }
}
