package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transaction_text")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionTextEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionTextPKEntity transactionTextPKEntity;
    @Column(name = "text")
    private String text;

}
