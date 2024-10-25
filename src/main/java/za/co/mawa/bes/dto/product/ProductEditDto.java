package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.product.category.ProductCategoryDto;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ProductEditDto {
    private String id;
    private String code;
    private String description;
    private String category;
    private String baseUnitOfMeasure;
    private BigDecimal price;
    private String pricingType;
    private List<ProductCategoryDto> categories;
}
