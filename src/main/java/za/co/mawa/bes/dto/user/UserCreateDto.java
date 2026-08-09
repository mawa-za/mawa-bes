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
    private String partnerId;
    private String accountType;
    private Boolean testUser;
    private Boolean protectedUser;
    private Boolean systemManaged;
    private String accessScope;
    private String environmentScope;
    private Boolean externalTransactionsBlocked;
    private java.util.Date expiresAt;
    private String protectedReason;
    private Boolean mfaRequired;

}
