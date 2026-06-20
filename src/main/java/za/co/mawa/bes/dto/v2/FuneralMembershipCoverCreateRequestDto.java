package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralMembershipCoverCreateRequestDto {

    private String identityNumber;
    private String membershipId;
    private String membershipNumber;
    private String burialSocietyPartnerId;
    private String burialSocietyName;
    private Long coverAmountCents;
    private String status;
}
