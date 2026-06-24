package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.transaction.TransactionLinkDto;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transaction_link")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class TransactionLinkEntity implements Serializable {


    private static final long serialVersionUID = 1L;

    @EmbeddedId
    protected TransactionLinkPKEntity transactionLinkPKEntity;

    @Column(name = "created_by")
    private String created_by;
    @Column(name = "creation_date")
    @Temporal(TemporalType.DATE)
    private Date creation_date;

}
