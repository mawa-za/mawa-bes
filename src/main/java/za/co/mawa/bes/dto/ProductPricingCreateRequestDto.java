package za.co.mawa.bes.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPricingCreateRequestDto {

    private BigDecimal value;
    private Date validFrom;
    private Date validTo;
}
