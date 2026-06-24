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
public class GroupSocietyCreateRequestDto {

    private String partnerId;
    private String groupNo;
    private String societyType;
    private String status;
    private Long availableBalanceCents;
    private Long totalPaidCents;
    private Long totalClaimedCents;
    private LocalDate lastPaymentDate;
    private LocalDate lastClaimDate;
}
