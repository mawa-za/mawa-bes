package za.co.mawa.bes.dto.transaction.amount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionAmountOutboundDto implements Serializable {
    private String id;
    private String transaction;
    private FieldOptionDto type;
    private BigDecimal amount = new BigDecimal("0.00");
    private String createdBy;
    private String changedBy;

}
