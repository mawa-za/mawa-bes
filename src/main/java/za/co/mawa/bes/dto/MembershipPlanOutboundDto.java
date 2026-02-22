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
public class MembershipPlanOutboundDto implements Serializable {
    private String planId;
    private String name;
    private String premiumCents;
    private boolean active;
    private String updatedAt;
}