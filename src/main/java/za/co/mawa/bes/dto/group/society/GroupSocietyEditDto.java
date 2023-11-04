package za.co.mawa.bes.dto.group.society;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupSocietyEditDto implements Serializable{
    private String status;
    private String statusReason;
    private String salesRepresentativeId;
    private BigDecimal premium;
    private String productId;
    private String previousProduct;
}
