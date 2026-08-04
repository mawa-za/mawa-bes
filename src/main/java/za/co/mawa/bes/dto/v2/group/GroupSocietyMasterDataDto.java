package za.co.mawa.bes.dto.v2.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSocietyMasterDataDto {
    private String id;
    private String partnerId;
    private String partnerNo;
    private String productId;
    private String productCode;
    private String productDescription;
    private String groupNo;
    private String name;
    private String societyType;
    private String status;
    private Long availableBalanceCents;
    private Long totalPaidCents;
    private LocalDate lastPaymentDate;
}
