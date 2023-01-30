package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_resources")
public class PartnerResourceApiEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    //@NotNull
    @Column(name = "resource_id", length = 20)
    private String resource_id;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_url", length = 20)
    private String partner_url;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_no", length = 45)
    private String partner_no;

    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;

    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "status_reason", length = 20)
    private String status_reason;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "port_number", length = 20)
    private String port_number;

    @Column(name = "resource_name", length = 45)
    private String resource_name;

    public PartnerResourceApiEntity() {
    }

    public PartnerResourceApiEntity(String resource_id) {
        this.resource_id = resource_id;
    }

    public String getResource_id() {
        return resource_id;
    }

    public void setResource_id(String resource_id) {
        this.resource_id = resource_id;
    }

    public String getPartner_url() {
        return partner_url;
    }

    public void setPartner_url(String partner_url) {
        this.partner_url = partner_url;
    }

    public String getPartner_no() {
        return partner_no;
    }

    public void setPartner_no(String partner_no) {
        this.partner_no = partner_no;
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

    public String getStatus_reason() {
        return status_reason;
    }

    public void setStatus_reason(String status_reason) {
        this.status_reason = status_reason;
    }

    public String getPort_number() {
        return port_number;
    }

    public void setPort_number(String port_number) {
        this.port_number = port_number;
    }

    public String getResource_name() {
        return resource_name;
    }

    public void setResource_name(String resource_name) {
        this.resource_name = resource_name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (resource_id != null ? resource_id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerResourceApiEntity)) {
            return false;
        }
        PartnerResourceApiEntity other = (PartnerResourceApiEntity) object;
        if ((this.resource_id == null && other.resource_id != null) || (this.resource_id != null && !this.resource_id.equals(other.resource_id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.ejb.entities.PartnerResourceApi[ resource_id=" + resource_id + " ]";
    }
}
