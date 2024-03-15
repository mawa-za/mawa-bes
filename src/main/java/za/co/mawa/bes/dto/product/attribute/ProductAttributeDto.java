package za.co.mawa.bes.dto.product.attribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ProductAttributeDto implements Serializable {
    private String product;
    private FieldOptionDto attribute;
    private String value;
    private String validFrom;
    private String validTo;
}
