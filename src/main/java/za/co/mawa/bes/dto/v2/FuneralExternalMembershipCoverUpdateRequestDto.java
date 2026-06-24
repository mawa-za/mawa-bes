package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralExternalMembershipCoverUpdateRequestDto {

    private String id;
    private String identityNumber;
    private String sourceTenantId;
    private String sourceTenantName;
    private String sourceMembershipId;
    private String sourceMembershipNo;
    private String sourceReference;
    private String burialSocietyPartnerId;
    private String burialSocietyName;
    private Long coverAmountCents;
    private String status;
    private LocalDateTime lastVerifiedAt;
}
