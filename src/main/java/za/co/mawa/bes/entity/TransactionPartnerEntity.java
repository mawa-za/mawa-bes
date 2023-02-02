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
    protected TransactionPartnerPKEntity transactionPartnerPKEntity;
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
    @Column(name = "status")
    private String status;
    @Column(name = "status_reason")
    private String statusReason;
    @Column(name = "createdBy")
    private String createdBy;
    @Column(name = "changedBy")
    private String changedBy;

    public TransactionPartnerEntity(TransactionPartnerDto transactionPartnerDto) {
    }
}
