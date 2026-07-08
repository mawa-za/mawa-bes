package za.co.mawa.bes.dto.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AdminHandoffResponseDto {
    private String tenant;
    private String tenantHost;
    private String tenantUrl;
    private String handoffToken;
    private String targetUrl;
    private Long expiresAt;
}
