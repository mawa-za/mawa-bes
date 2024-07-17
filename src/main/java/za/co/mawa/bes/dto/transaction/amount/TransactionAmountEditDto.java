package za.co.mawa.bes.dto.transaction.amount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionAmountEditDto implements Serializable {
    private String id;
    private String transaction;
    private String type;
    private BigDecimal amount;
}
