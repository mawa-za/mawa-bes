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
public class TransactionAmountCreateRequestDto {

    private String transaction;
    private String type;
    private BigDecimal amount;
    private String changedBy;
}
