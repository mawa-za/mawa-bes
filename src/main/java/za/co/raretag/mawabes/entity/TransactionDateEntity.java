package za.co.raretag.mawabes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "transaction_date")
public class TransactionDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionDatePKEntity transactionDatePK;
    @Column(name = "value")
    @Temporal(TemporalType.TIMESTAMP)
    private Date value;

    public TransactionDateEntity() {
    }

    public TransactionDateEntity(TransactionDatePKEntity transactionDatePK) {
        this.transactionDatePK = transactionDatePK;
    }

    public TransactionDateEntity(String transaction, String type) {
        this.transactionDatePK = new TransactionDatePKEntity(transaction, type);
    }

    public TransactionDatePKEntity getTransactionDatePK() {
        return transactionDatePK;
    }

    public void setTransactionDatePK(TransactionDatePKEntity transactionDatePK) {
        this.transactionDatePK = transactionDatePK;
    }

    public Date getValue() {
        return value;
    }

    public void setValue(Date value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionDatePK != null ? transactionDatePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TransactionDateEntity)) {
            return false;
        }
        TransactionDateEntity other = (TransactionDateEntity) object;
        if ((this.transactionDatePK == null && other.transactionDatePK != null) || (this.transactionDatePK != null && !this.transactionDatePK.equals(other.transactionDatePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawabes.entity.TransactionDateEntity[ transactionDatePK=" + transactionDatePK + " ]";
    }

}
