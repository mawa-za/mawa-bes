package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_bank_account")
public class PartnerBankingDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerBankingDetailsPKEntity partnerBankingDetailsPk;
    @Size(max = 20)
    @Column(name = "account_holder")
    private String accountHolder;
    @Size(max = 20)
    @Column(name = "account_type")
    private String accountType;
    @Size(max = 20)
    @Column(name = "bank_name")
    private String bankName;
    @Size(max = 20)
    @Column(name = "branch_code")
    private String branchCode;
    @Size(max = 20)
    @Column(name = "branch_name")
    private String branchName;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Size(max = 20)
    @Column(name = "status")
    private String status;

    public PartnerBankingDetailsEntity() {
    }

    public PartnerBankingDetailsEntity(PartnerBankingDetailsPKEntity partnerBankingDetailsPk) {
        this.partnerBankingDetailsPk = partnerBankingDetailsPk;
    }

    public PartnerBankingDetailsEntity(String partner, String type, String account_number) {
        this.partnerBankingDetailsPk = new PartnerBankingDetailsPKEntity(partner, type, account_number);
    }

    public PartnerBankingDetailsPKEntity getPartnerBankingDetailsPk() {
        return partnerBankingDetailsPk;
    }

    public void setPartnerBankingDetailsPk(PartnerBankingDetailsPKEntity partnerBankingDetailsPk) {
        this.partnerBankingDetailsPk = partnerBankingDetailsPk;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partnerBankingDetailsPk != null ? partnerBankingDetailsPk.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerBankingDetailsEntity)) {
            return false;
        }
        PartnerBankingDetailsEntity other = (PartnerBankingDetailsEntity) object;
        if ((this.partnerBankingDetailsPk == null && other.partnerBankingDetailsPk != null) || (this.partnerBankingDetailsPk != null && !this.partnerBankingDetailsPk.equals(other.partnerBankingDetailsPk))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "za.raretag.mawa.entities.PartnerBankingDetails[ partnerBankingDetailsPk=" + partnerBankingDetailsPk + " ]";
    }

}
