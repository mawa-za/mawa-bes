package za.co.mawa.bes.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStorageBinResponseDto {

    private String id;
    private BigDecimal minQty;
    private BigDecimal maxQty;
}
