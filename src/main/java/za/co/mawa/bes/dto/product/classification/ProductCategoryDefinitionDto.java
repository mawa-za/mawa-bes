package za.co.mawa.bes.dto.product.classification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryDefinitionDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private String parentId;
    private String parentCode;
    private String parentName;
    private String productType;
    private String fullPath;
    private boolean active;
    private int sortOrder;
}
