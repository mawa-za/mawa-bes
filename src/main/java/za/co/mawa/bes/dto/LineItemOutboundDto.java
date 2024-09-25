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
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private boolean isVatInclusive;

    private BigDecimal totExcVat;
    private BigDecimal totIncVat;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal VatAmount;
    private BigDecimal VatPercentage;
}
