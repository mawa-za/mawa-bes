package za.co.raretag.mawabes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;


import java.io.Serializable;

public class TransactionDatePKEntity implements Serializable {

    @Basic(optional = false)

    @Column(name = "transaction", length = 20)
    private String transaction;
    @Basic(optional = false)

    @Column(name = "type", length = 20)
    private String type;

    public TransactionDatePKEntity() {
    }

    public TransactionDatePKEntity(String transaction, String type) {
        this.transaction = transaction;
        this.type = type;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transaction != null ? transaction.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TransactionDatePKEntity)) {
            return false;
        }
        TransactionDatePKEntity other = (TransactionDatePKEntity) object;
        if ((this.transaction == null && other.transaction != null) || (this.transaction != null && !this.transaction.equals(other.transaction))) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawabes.entity.TransactionDatePKEntity[ transaction=" + transaction + ", type=" + type + " ]";
    }
}
