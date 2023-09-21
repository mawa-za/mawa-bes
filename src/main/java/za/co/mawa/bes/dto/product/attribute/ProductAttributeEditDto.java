package za.co.mawa.bes.dto.product.attribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ProductAttributeEditDto implements Serializable {
    private String value;
    private String validFrom;
    private String validTo;
}
