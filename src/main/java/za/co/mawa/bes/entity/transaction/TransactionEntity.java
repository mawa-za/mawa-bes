package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tebogomohale
 */
@Entity
@Table(name = "transaction", uniqueConstraints = { @UniqueConstraint(name = "UniqueNumberAndType", columnNames = { "number", "type" }) })
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "number")
    private String number;
    @Column(name = "type")
    private String type;
    @Column(name = "sub_type")
    private String subType;
    @Column(name = "description")
    private String description;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status")
    private String status;
    @Column(name = "status_reason")
    private String statusReason;
    @Column(name = "sub_status")
    private String subStatus;
    @Column(name = "location")
    private String location;
    @Lob
    @Column(name = "sub_description")
    private String subDescription;
    @Column(name = "createdBy")
    private String createdBy;
    @Column(name = "changedBy")
    private String changedBy;

    public TransactionEntity(TransactionDto transactionDto) {
        this.number = transactionDto.getNumber();
        this.type = transactionDto.getType();
        this.subType = transactionDto.getSubType();
        this.description = transactionDto.getDescription();
        this.status = transactionDto.getStatus();
    }

    public TransactionEntity(TransactionCreateDto transactionCreateDto) {
         this.type = transactionCreateDto.getType();
        this.subType = transactionCreateDto.getSubType();
        this.description = transactionCreateDto.getDescription();
        this.status = transactionCreateDto.getStatus();
    }
}


