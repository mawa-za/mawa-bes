package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class PartnerBankingDetailsPKEntity implements Serializable {
    @Basic(optional = false)
    @Column(name = "partner")
    private String partner;
    @Basic(optional = false)
    @Column(name = "type")
    private String type;

    @Column(name = "account_number")
    private String accountNumber;

    public PartnerBankingDetailsPKEntity() {
    }

    public PartnerBankingDetailsPKEntity(String partner, String type, String accountNumber) {
        this.partner = partner;
        this.type = type;
        this.accountNumber = accountNumber;

    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partner != null ? partner.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        hash += (accountNumber != null ? accountNumber.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(obj instanceof PartnerBankingDetailsPKEntity)) {
            return false;
        }
        PartnerBankingDetailsPKEntity other = (PartnerBankingDetailsPKEntity) obj;
        if ((this.partner == null && other.partner != null) || (this.partner != null && !this.partner.equals(other.partner))) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        if ((this.accountNumber == null && other.accountNumber != null) || (this.accountNumber != null && !this.accountNumber.equals(other.accountNumber))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "za.raretag.mawa.entities.PartnerBankingDetailsPK[ partner=" + partner + ", type=" + type + ", accountNumber=" + accountNumber + " ]";
    }
}
