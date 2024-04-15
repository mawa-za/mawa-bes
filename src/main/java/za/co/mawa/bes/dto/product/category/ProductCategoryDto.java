package za.co.mawa.bes.dto.product.category;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ProductCategoryDto implements Serializable {
    private String product;
    private FieldOptionDto category;
    private Date validFrom;
    private Date validTo;
}
