package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.product.ProductBasicDto;
import za.co.mawa.bes.dto.product.ProductDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class LineItemOutboundDto implements Serializable {
    private String item;
    private String transaction;
    private ProductDto product;
    private String barcode;
    private FieldOptionDto uom;
    private BigDecimal quantity  = new BigDecimal("0");
    private BigDecimal unitPrice = new BigDecimal("0");
    private BigDecimal lineTotal = new BigDecimal("0");
    private BigDecimal vatAmount = new BigDecimal("0");
    private BigDecimal totIncVat = new BigDecimal("0");
    private BigDecimal totExcVat = new BigDecimal("0");
    private BigDecimal vatPercentage = new BigDecimal("0");
    private BigDecimal discountAmount = new BigDecimal("0");
    private BigDecimal discountPercentage = new BigDecimal("0");
}
