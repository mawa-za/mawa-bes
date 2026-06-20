package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import za.co.mawa.bes.enums.PremiumStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPremiumUpdateRequestDto {

    private String id;
    private String membershipId;
    private String periodYYYYMM;
    private Long amountCents;
    private Long paidAmountCents;
    private Long balanceCents;
    private PremiumStatus status;
    private LocalDate dueDate;
}
