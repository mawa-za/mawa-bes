package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserDto {
    private String id;
    private String username;
    private String password;
    private String email;
    private String cellphone;
    private String type;
    private String status;

}
