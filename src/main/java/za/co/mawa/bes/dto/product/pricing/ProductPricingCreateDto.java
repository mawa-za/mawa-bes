package za.co.mawa.bes.dto.product.pricing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ProductPricingCreateDto {
    private String product;
    private String pricing;
    private BigDecimal value;
    private Date validFrom;
    private Date validTo;
}
