package za.co.mawa.bes.dto.transaction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionDateEntity;

import java.io.Serializable;
import java.util.Date;
@NoArgsConstructor
@Getter
@Setter
public class TransactionDateDto implements Serializable {
    private String transaction;
    private String type;
    private Date value;
    public TransactionDateDto(TransactionDateEntity transactionDateEntity) {
        this.transaction = transactionDateEntity.getTransactionDatePKEntity().getTransaction();
        this.type = transactionDateEntity.getTransactionDatePKEntity().getType();
        this.value = transactionDateEntity.getValue();
    }

}
