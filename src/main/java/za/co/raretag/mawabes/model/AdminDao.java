package za.co.raretag.mawabes.model;

public interface AdminDao {
    String authenticate(JwtRequest authenticationRequest);
}
