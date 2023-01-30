package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.TransactionPartnerDto;


import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "transaction_partner")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionPartnerEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionPartnerPKEntity transactionPartnerPK;
    @Column(name = "date_added")
    @Temporal(TemporalType.DATE)
    private Date dateAdded;
    @Column(name = "date_effective")
    @Temporal(TemporalType.DATE)
    private Date dateEffective;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "status_reason", length = 20)
    private String statusReason;
    @Column(name = "createdBy",length = 45)
    private String createdBy;
    @Column(name = "changedBy",length = 45)
    private String changedBy;

    public TransactionPartnerEntity(TransactionPartnerDto transactionPartnerDto) {
    }
}
