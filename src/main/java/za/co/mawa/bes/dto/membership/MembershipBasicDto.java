package za.co.mawa.bes.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerBasicDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MembershipBasicDto {
    private String id;
    private String number;
    private PartnerBasicDto member;
    private PartnerBasicDto salesRepresentative;
    private ProductBasicDto product;
    private BigDecimal premium;
    private Date dateJoined;
    private Date dateEffective;
    private String status;
    private String statusReason;
}
