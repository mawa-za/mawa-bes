package za.co.mawa.bes.dto.access;
import lombok.*;
import java.util.Date;
import java.util.List;
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder
public class UserAccessProfileDto {
    private String userId;
    private String username;
    private String displayName;
    private String email;
    private String accountType;
    private Boolean testUser;
    private Boolean protectedUser;
    private Boolean systemManaged;
    private String accessScope;
    private String environmentScope;
    private Boolean externalTransactionsBlocked;
    private Date expiresAt;
    private Boolean mfaRequired;
    private Boolean platformSession;
    private String platformUserId;
    private String handoffId;
    private String accessReason;
    private String ticketReference;
    private String tenantId;
    private List<String> roles;
    private Boolean allWorkcentres;
}
