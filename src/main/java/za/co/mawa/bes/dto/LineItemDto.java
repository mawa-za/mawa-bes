package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@NoArgsConstructor
@Getter
@Setter
public class LineItemDto {
    private String productId;
    private String code;
    private String description;
    private String ean;
    private String uom;
    private BigDecimal quantity  = new BigDecimal("0");
    private BigDecimal unitPrice = new BigDecimal("0");
    private BigDecimal lineTotal = new BigDecimal("0");
}
