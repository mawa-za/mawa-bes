package za.co.mawa.bes.dto.transaction.amount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;

import java.io.Serializable;
import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionAmountDto implements Serializable {
    private String transaction;
    private String type;
    private BigDecimal amount = new BigDecimal("0.00");

    public TransactionAmountDto(String transaction, String type) {
        this.transaction = transaction;
        this.type = type;
    }

    public TransactionAmountDto(TransactionAmountEntity transactionAmountEntity) {
        this.transaction = transactionAmountEntity.getTransactionAmountPKEntity().getTransaction();
        this.type = transactionAmountEntity.getTransactionAmountPKEntity().getType();
        this.amount = transactionAmountEntity.getAmount();
    }

}
