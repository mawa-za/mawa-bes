package za.co.mawa.bes.dto.product.classification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCategoryCreateRequestDto {
    private String code;
    private String name;
    private String description;
    private String parentId;
    private String productType;
    private Boolean active;
    private Integer sortOrder;
}
