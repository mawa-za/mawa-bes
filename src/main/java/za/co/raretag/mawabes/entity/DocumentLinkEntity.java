package za.co.raretag.mawabes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "document_link")
public class DocumentLinkEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DocumentLinkPKEntity documentLinkPKEntity;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public DocumentLinkEntity() {
    }

    public DocumentLinkEntity(DocumentLinkPKEntity documentLinkPKEntity) {
        this.documentLinkPKEntity = documentLinkPKEntity;
    }

    public DocumentLinkEntity(String parent, String child, String type) {
        this.documentLinkPKEntity = new DocumentLinkPKEntity(parent, child, type);
    }

    public DocumentLinkPKEntity getDocumentLinkPK() {
        return documentLinkPKEntity;
    }

    public void setDocumentLinkPK(DocumentLinkPKEntity documentLinkPKEntity) {
        this.documentLinkPKEntity = documentLinkPKEntity;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (documentLinkPKEntity != null ? documentLinkPKEntity.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DocumentLinkEntity)) {
            return false;
        }
        DocumentLinkEntity other = (DocumentLinkEntity) object;
        if ((this.documentLinkPKEntity == null && other.documentLinkPKEntity != null) || (this.documentLinkPKEntity != null && !this.documentLinkPKEntity.equals(other.documentLinkPKEntity))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.entities.DocumentLink[ documentLinkPK=" + documentLinkPKEntity + " ]";
    }
}
