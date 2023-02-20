package za.co.mawa.bes.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class UserCreateDto implements Serializable {
    private String username;
    private String password;
    private String email;
    private String cellphone;
    private String userType;

}
