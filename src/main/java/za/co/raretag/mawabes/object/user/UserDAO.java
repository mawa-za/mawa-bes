package za.co.raretag.mawabes.object.user;

public interface UserDAO {
    boolean authenticate(UserDTO userDTO);
}
