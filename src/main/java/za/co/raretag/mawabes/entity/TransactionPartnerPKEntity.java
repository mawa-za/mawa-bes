package za.co.raretag.mawabes.entity;


import jakarta.persistence.*;

import java.io.Serializable;

public class TransactionPartnerPKEntity implements Serializable {

    @Basic(optional = false)

    @Column(name = "transaction_id", length = 20)
    private String transactionId;
    @Basic(optional = false)

    @Column(name = "partner_function", length = 20)
    private String partnerFunction;
    @Basic(optional = false)

    @Column(name = "partner_no", length = 20)
    private String partnerNo;

    public TransactionPartnerPKEntity() {
    }

    public TransactionPartnerPKEntity(String transactionId, String partnerFunction, String partnerNo) {
        this.transactionId = transactionId;
        this.partnerFunction = partnerFunction;
        this.partnerNo = partnerNo;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPartnerFunction() {
        return partnerFunction;
    }

    public void setPartnerFunction(String partnerFunction) {
        this.partnerFunction = partnerFunction;
    }

    public String getPartnerNo() {
        return partnerNo;
    }

    public void setPartnerNo(String partnerNo) {
        this.partnerNo = partnerNo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionId != null ? transactionId.hashCode() : 0);
        hash += (partnerFunction != null ? partnerFunction.hashCode() : 0);
        hash += (partnerNo != null ? partnerNo.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TransactionPartnerPKEntity)) {
            return false;
        }
        TransactionPartnerPKEntity other = (TransactionPartnerPKEntity) object;
        if ((this.transactionId == null && other.transactionId != null) || (this.transactionId != null && !this.transactionId.equals(other.transactionId))) {
            return false;
        }
        if ((this.partnerFunction == null && other.partnerFunction != null) || (this.partnerFunction != null && !this.partnerFunction.equals(other.partnerFunction))) {
            return false;
        }
        if ((this.partnerNo == null && other.partnerNo != null) || (this.partnerNo != null && !this.partnerNo.equals(other.partnerNo))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.TransactionPartnerPK[ transactionId=" + transactionId + ", partnerFunction=" + partnerFunction + ", partnerNo=" + partnerNo + " ]";
    }
}
