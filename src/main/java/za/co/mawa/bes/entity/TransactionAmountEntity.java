package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.TransactionAmountDto;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_amount")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionAmountEntity {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionAmountPKEntity transactionAmountPKEntity;
    @Column(name = "amount")
    private BigDecimal amount;

    public TransactionAmountEntity(TransactionAmountDto transactionAmountDto) {
    }
}
