package za.co.mawa.bes.dto.group.society;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyQueryDto {
    private String memberId;
    private String member;
    private String salesRepresentativeId;
    private String salesRepresentative;
    private String productId;
    private String productDescription;
    private BigDecimal premium;
    private Date dateJoined;
    private Date dateEffective;
    private String status;
    private String statusReason;
}
