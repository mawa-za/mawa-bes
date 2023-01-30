package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.TransactionDateEntity;

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
        this.transaction = transactionDateEntity.getTransactionDatePK().getTransaction();
        this.type = transactionDateEntity.getTransactionDatePK().getType();
        this.value = transactionDateEntity.getValue();
    }

}
