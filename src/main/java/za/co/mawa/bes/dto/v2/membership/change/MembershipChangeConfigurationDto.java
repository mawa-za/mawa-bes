package za.co.mawa.bes.dto.v2.membership.change;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MembershipChangeConfigurationDto { private Integer planChangeWaitingPeriodMonths; private String updatedBy; }
