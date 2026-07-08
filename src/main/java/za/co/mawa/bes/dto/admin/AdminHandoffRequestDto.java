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
    private String redirectPath;
}
