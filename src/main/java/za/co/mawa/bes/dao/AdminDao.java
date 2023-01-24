package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.JwtRequest;

public interface AdminDao {
    String authenticate(JwtRequest authenticationRequest);
}
