package za.co.mawa.bes.dto.product.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductPricingQueryDto implements Serializable {
    private String product;
    private String pricing;
}
