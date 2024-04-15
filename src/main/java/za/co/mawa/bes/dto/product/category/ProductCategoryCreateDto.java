package za.co.mawa.bes.dto.product.category;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ProductCategoryCreateDto implements Serializable {
    private String product;
    private String category;

}
