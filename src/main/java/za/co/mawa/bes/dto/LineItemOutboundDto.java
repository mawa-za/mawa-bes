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
    private BigDecimal totalExcVat = new BigDecimal("0");
    private BigDecimal totalIncVat = new BigDecimal("0");
    private BigDecimal discountAmount = new BigDecimal("0");
    private BigDecimal discountPercentage = new BigDecimal("0");
    private BigDecimal VATAmount = new BigDecimal("0");
    private BigDecimal VATPercentage = new BigDecimal("0");
}
