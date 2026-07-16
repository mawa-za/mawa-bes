package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthenticationResponseDto implements Serializable {
    private String userId;
    private String username;
    private String displayName;
    private String accessToken;
    private String refreshToken;
    private String accountType;
    private Boolean testUser;
    private Boolean protectedUser;
    private String accessScope;
    private Boolean platformSession;
    private String platformUserId;
    private String tenantId;
    private String roleId;
    private String roleDescription;
    private Boolean externalTransactionsBlocked;
    private java.util.Date expiresAt;
    private String handoffId;
    private String accessReason;
    private String ticketReference;
}

