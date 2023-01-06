package za.co.raretag.mawabes.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

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

    public TransactionPartnerEntity() {
    }

    public TransactionPartnerEntity(TransactionPartnerPKEntity transactionPartnerPK) {
        this.transactionPartnerPK = transactionPartnerPK;
    }

    public TransactionPartnerEntity(String transactionId, String partnerFunction, String partnerNo) {
        this.transactionPartnerPK = new TransactionPartnerPKEntity(transactionId, partnerFunction, partnerNo);
    }

    public TransactionPartnerPKEntity getTransactionPartnerPK() {
        return transactionPartnerPK;
    }

    public void setTransactionPartnerPK(TransactionPartnerPKEntity transactionPartnerPK) {
        this.transactionPartnerPK = transactionPartnerPK;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Date getDateEffective() {
        return dateEffective;
    }

    public void setDateEffective(Date dateEffective) {
        this.dateEffective = dateEffective;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionPartnerPK != null ? transactionPartnerPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TransactionPartnerEntity)) {
            return false;
        }
        TransactionPartnerEntity other = (TransactionPartnerEntity) object;
        if ((this.transactionPartnerPK == null && other.transactionPartnerPK != null) || (this.transactionPartnerPK != null && !this.transactionPartnerPK.equals(other.transactionPartnerPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.TransactionPartner[ transactionPartnerPK=" + transactionPartnerPK + " ]";
    }
}
