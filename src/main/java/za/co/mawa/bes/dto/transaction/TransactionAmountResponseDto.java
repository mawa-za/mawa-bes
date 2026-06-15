package za.co.mawa.bes.dto.transaction;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAmountResponseDto {

    private String id;
    private String transaction;
    private String type;
    private BigDecimal amount;
    private String createdBy;
    private String changedBy;
}
