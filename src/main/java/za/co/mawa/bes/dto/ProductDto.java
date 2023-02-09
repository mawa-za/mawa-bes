package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.ProductEntity;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductDto {
    private String id;
    private String code;
    private String description;
    private String category;
    private BigDecimal sellingPrice;
    private String baseUnitOfMeasure;

    public ProductDto(ProductEntity productEntity){
        this.id = productEntity.getId();
        this.code = productEntity.getCode();
        this.description = productEntity.getDescription();
        this.category = productEntity.getCategory();
    }
}
