package za.co.raretag.mawabes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class DocumentLinkPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "parent", length = 40)
    private String parent;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "child", length = 40)
    private String child;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;

    public DocumentLinkPKEntity() {
    }

    public DocumentLinkPKEntity(String parent, String child, String type) {
        this.parent = parent;
        this.child = child;
        this.type = type;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getChild() {
        return child;
    }

    public void setChild(String child) {
        this.child = child;
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
        hash += (parent != null ? parent.hashCode() : 0);
        hash += (child != null ? child.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DocumentLinkPKEntity)) {
            return false;
        }
        DocumentLinkPKEntity other = (DocumentLinkPKEntity) object;
        if ((this.parent == null && other.parent != null) || (this.parent != null && !this.parent.equals(other.parent))) {
            return false;
        }
        if ((this.child == null && other.child != null) || (this.child != null && !this.child.equals(other.child))) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.entities.DocumentLinkPK[ parent=" + parent + ", child=" + child + ", type=" + type + " ]";
    }
}
