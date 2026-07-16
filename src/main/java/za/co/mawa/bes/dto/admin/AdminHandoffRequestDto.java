package za.co.mawa.bes.dto.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AdminHandoffRequestDto {
    private String tenant;
    private String tenantHost;
    private String tenantUrl;
    private String adminUsername;
    private String platformUserId;
    private String displayName;
    private String email;
    private String accountType;
    private String platformScope;
    private Boolean testUser;
    private Boolean protectedUser;
    private Boolean externalTransactionsBlocked;
    private java.util.Date expiresAt;
    private java.util.List<String> roleIds;
    private String accessReason;
    private String ticketReference;
    private String redirectPath;
}
