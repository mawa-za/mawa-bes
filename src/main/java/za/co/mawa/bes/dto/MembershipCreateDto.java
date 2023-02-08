package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipCreateDto implements Serializable {
    private String memberId;
    private String salesRepresentativeId;
    private String productId;
    private BigDecimal premium;
    private Date dateJoined;
    private Date dateEffective;
    private String statusId;
    private String statusReasonId;
}
