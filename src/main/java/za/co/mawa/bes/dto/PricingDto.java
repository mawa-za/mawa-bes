package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class PricingDto {
    private BigDecimal totalExcVat;
    private BigDecimal totalIncVat;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal VATAmount;
    private BigDecimal VATPercentage;

}
