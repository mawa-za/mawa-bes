package za.co.mawa.bes.dto.product;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryCreateRequestDto {

    private String product;
    private String category;
    private Date validFrom;
    private Date validTo;
}
