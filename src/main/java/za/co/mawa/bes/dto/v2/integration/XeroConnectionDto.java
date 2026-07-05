package za.co.mawa.bes.dto.v2.integration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class XeroConnectionDto {
    private String id;
    private String tenantId;
    private String tenantName;
    private String tenantType;
    private String createdDateUtc;
    private String updatedDateUtc;
}
