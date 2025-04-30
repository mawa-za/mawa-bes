package za.co.mawa.bes.dto.membership;
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
public class MembershipEditDto implements Serializable{
    private String status;
    private String statusReason;
    private String salesRepresentativeId;
    private BigDecimal premium;
    private String productId;
    private String previousProduct;
}
