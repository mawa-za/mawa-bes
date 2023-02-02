package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.TransactionDateDto;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transaction_date")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionDatePKEntity transactionDatePKEntity;
    @Column(name = "value")
    @Temporal(TemporalType.TIMESTAMP)
    private Date value;

    public TransactionDateEntity(TransactionDateDto transactionDateDto) {
        this.transactionDatePKEntity = new TransactionDatePKEntity();
        this.transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        this.transactionDatePKEntity.setType(transactionDateDto.getType());
        this.value = (transactionDateDto.getValue());
    }

}
