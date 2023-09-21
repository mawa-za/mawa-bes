package za.co.mawa.bes.dto.product.attribute;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ProductAttributeQueryDto implements Serializable {
    private String product;
    private String attribute;
    private String value;
    private Date validTo;
    private Date validFrom;
}
