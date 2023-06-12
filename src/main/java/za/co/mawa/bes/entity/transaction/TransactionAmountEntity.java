package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_amount")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionAmountEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionAmountPKEntity transactionAmountPKEntity;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "changed_by")
    private String changedBy;

    public TransactionAmountEntity(TransactionAmountDto transactionAmountDto) {
        this.transactionAmountPKEntity.setTransaction(transactionAmountDto.getTransaction());
        this.transactionAmountPKEntity.setType(transactionAmountDto.getType());
        this.amount = transactionAmountDto.getAmount();
    }
}
