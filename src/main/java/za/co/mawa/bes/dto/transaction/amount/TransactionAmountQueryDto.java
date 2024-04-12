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
public class TransactionAmountQueryDto implements Serializable {
    private String transaction;
    private String type;
}
