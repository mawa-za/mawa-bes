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
    private FieldOptionDto pricing;
    private BigDecimal value;
    private Date validFrom;
    private Date validTo;

    private BigDecimal totalExcVat = new BigDecimal("0");
    private BigDecimal totalIncVat = new BigDecimal("0");
    private BigDecimal discountAmount = new BigDecimal("0");
    private BigDecimal discountPercentage = new BigDecimal("0");
    private BigDecimal VATAmount = new BigDecimal("0");
    private BigDecimal VATPercentage = new BigDecimal("0");


}
