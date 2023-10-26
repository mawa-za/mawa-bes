package za.co.mawa.bes.dto.product.pricing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ProductPricingEditDto implements Serializable {
    private String product;
    private String pricing;
    private BigDecimal value;
    private Date validFrom;
    private Date validTo;
}
