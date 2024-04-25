package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PricingInboundDto {
    private BigDecimal totalExcVat = new BigDecimal("0");
    private BigDecimal totalIncVat = new BigDecimal("0");
    private BigDecimal discountAmount = new BigDecimal("0");
    private BigDecimal discountPercentage = new BigDecimal("0");
    private BigDecimal VATAmount = new BigDecimal("0");
    private BigDecimal VATPercentage = new BigDecimal("0");

}
