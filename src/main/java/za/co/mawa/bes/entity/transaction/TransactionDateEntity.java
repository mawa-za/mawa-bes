package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transaction_date")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class TransactionDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionDatePKEntity transactionDatePKEntity;
    @Column(name = "value")
    @Temporal(TemporalType.TIMESTAMP)
    private Date value;

}
