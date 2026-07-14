package za.co.mawa.bes.dto.v2.payapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAppPlanSyncDto {
    private String id;
    private String planCode;
    private String name;
    private String description;
    private Long premiumCents;
    private Boolean active;
}
