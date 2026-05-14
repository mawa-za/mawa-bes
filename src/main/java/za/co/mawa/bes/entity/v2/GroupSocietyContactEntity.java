package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;

@Entity
@Table(name = "group_society_contact")
public class GroupSocietyContactEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "group_society_id", nullable = false)
    private String groupSocietyId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    private String role;

    @Column(name = "mobile_no")
    private String mobileNo;

    private String email;

    @Column(name = "primary_contact", nullable = false)
    private Boolean primaryContact = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private Date updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public String getId() {
        return id;
    }

    public String getGroupSocietyId() {
        return groupSocietyId;
    }

    public void setGroupSocietyId(String groupSocietyId) {
        this.groupSocietyId = groupSocietyId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Boolean primaryContact) {
        this.primaryContact = primaryContact;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}