package za.co.mawa.bes.dto.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class ProductEditDto {
    private String id;
    private String code;
    private String description;
    private String vatInclusive;
    private String category;
    private String baseUnitOfMeasure;
}