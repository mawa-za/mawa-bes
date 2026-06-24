package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyResponseDto {

    private String id;
    private String partnerId;
    private String groupNo;
    private String societyType;
    private String status;
    private Long availableBalanceCents;
    private Long totalPaidCents;
    private Long totalClaimedCents;
    private LocalDate lastPaymentDate;
    private LocalDate lastClaimDate;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
}
