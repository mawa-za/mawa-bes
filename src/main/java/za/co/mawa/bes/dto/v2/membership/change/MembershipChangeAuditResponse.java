package za.co.mawa.bes.dto.v2.membership.change;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MembershipChangeAuditResponse {
 private String id; private String membershipId; private String changeRequestId; private String eventType;
 private String oldValuesJson; private String newValuesJson; private String details; private String performedBy; private LocalDateTime performedAt;
}
