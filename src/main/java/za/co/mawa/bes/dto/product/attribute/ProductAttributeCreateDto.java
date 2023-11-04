package za.co.mawa.bes.dto.product.attribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ProductAttributeCreateDto implements Serializable {
    private String product;
    private String attribute;
    private String value;

}
