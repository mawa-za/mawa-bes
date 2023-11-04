package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ProductDto {
    private String id;
    private String code;
    private String description;
    private String category;
    private String categoryDescription;
    private BigDecimal sellingPrice;
    private String baseUnitOfMeasure;
    private String baseUnitOfMeasureDescription;
    private String validTo;
    private String validFrom;
    private String priceType;
    private String priceTypeDescription;
    private List<ProductPricingDto> pricings;
    private List<ProductAttributeDto> attributes;


    public ProductDto(ProductEntity productEntity){
        this.id = productEntity.getId();
        this.code = productEntity.getCode();
        this.description = productEntity.getDescription();
        this.category = productEntity.getCategory();
    }
}
