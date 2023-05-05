package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductUpdateDto {
    private String code;
    private String description;
    private String category;
   private String baseUnitOfMeasure;
   private BigDecimal price;
    private String pricingType;
}
