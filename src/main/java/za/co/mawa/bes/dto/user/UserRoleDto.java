package za.co.mawa.bes.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class UserRoleDto implements Serializable {
    private String user;
    private String role;
}
