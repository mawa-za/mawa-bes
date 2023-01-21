package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

    /**
     *
     * @author tebogomohale
     */
    @Entity
    @Table(name = "transaction")
    public class TransactionEntity implements Serializable {
        private static final long serialVersionUID = 1L;
        @Id
        @Basic(optional = false)
        @Column(name = "id", length = 20)
        private String id;
        @Column(name = "type", length = 20)
        private String type;
        @Column(name = "sub_type", length = 45)
        private String subType;
        @Column(name = "description", length = 255)
        private String description;
        @Column(name = "valid_from")
        @Temporal(TemporalType.DATE)
        private Date validFrom;
        @Column(name = "valid_to")
        @Temporal(TemporalType.DATE)
        private Date validTo;
        @Column(name = "status", length = 45)
        private String status;
        @Column(name = "status_reason", length = 45)
        private String statusReason;
        @Column(name = "sub_status", length = 45)
        private String subStatus;
        @Column(name = "location", length = 100)
        private String location;
        @Lob
        @Column(name = "sub_description")
        private String subDescription;
        @Column(name = "createdBy", length = 45)
        private String createdBy;
        @Column(name = "changedBy", length = 45)
        private String changedBy;

        public TransactionEntity() {
        }

        public TransactionEntity(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSubType() {
            return subType;
        }

        public void setSubType(String subType) {
            this.subType = subType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
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

        public String getSubStatus() {
            return subStatus;
        }

        public void setSubStatus(String subStatus) {
            this.subStatus = subStatus;
        }

        public String getStatusReason() {
            return statusReason;
        }

        public void setStatusReason(String statusReason) {
            this.statusReason = statusReason;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getSubDescription() {
            return subDescription;
        }

        public void setSubDescription(String subDescription) {
            this.subDescription = subDescription;
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
            hash += (id != null ? id.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            // TODO: Warning - this method won't work in the case the id fields are not set
            if (!(object instanceof TransactionEntity)) {
                return false;
            }
            TransactionEntity other = (TransactionEntity) object;
            if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "za.co.raretag.mawa.entities.Transaction[ id=" + id + " ]";
        }

    }


