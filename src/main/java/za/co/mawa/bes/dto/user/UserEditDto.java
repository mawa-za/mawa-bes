package za.co.mawa.bes.dto.user;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class UserEditDto implements Serializable{
    private String cellphone;
    private String email;
    private String password;
    private String userType;
    private String status;
    private String statusReason;
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
