package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ProductDto {
    private String id;
    private String code;
    private String description;
    private FieldOptionDto type;
    private FieldOptionDto category;
    private FieldOptionDto baseUnitOfMeasure;
    private Date validTo;
    private Date validFrom;
    private List<ProductPricingDto> pricings;
    private List<ProductAttributeDto> attributes;
    private List<ProductCategoryDto> categories;
}
