package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
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
@EqualsAndHashCode
public class TransactionAmountEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "transaction")
    private String transaction;
    @Column(name = "type")
    private String type;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "changed_by")
    private String changedBy;
}
