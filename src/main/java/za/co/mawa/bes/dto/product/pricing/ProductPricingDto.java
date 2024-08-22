package za.co.mawa.bes.dto.product.pricing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ProductPricingDto {
    private String product;
    private String vatInclusive;
    private FieldOptionDto pricing;
    private BigDecimal value;
    private Date validFrom;
    private Date validTo;
    private BigDecimal totIncVat;
    private BigDecimal totExcVat;
    private BigDecimal vatAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
}
