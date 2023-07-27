package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class LineItemEditDto implements Serializable {
    //private String productId;
    private String uom;
    private BigDecimal quantity  = new BigDecimal("0");
    private BigDecimal unitPrice = new BigDecimal("0");
}
