package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipCreateRequestDto {

    private String memberId;
    private String membershipNo;
    private String planId;
    private Long premiumCents;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String paidUpToPeriod;
    private LocalDate joinDate;
    private String oldId;
}
