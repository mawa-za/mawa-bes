package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductCreateDto {
    private String code;
    private String description;
    private String type;
    private String category;
    private String baseUnitOfMeasure;
    private BigDecimal price;
    private String pricingType;
    private String autoGenerateCode;
    private String attribute;
    private String value;
}
