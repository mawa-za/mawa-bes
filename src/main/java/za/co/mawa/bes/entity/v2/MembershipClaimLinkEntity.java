package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_claim_link")
public class MembershipClaimLinkEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "parent_claim_id", nullable = false, length = 255)
    private String parentClaimId;

    @Column(name = "linked_claim_id", nullable = false, length = 255)
    private String linkedClaimId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getParentClaimId() {
        return parentClaimId;
    }

    public void setParentClaimId(String parentClaimId) {
        this.parentClaimId = parentClaimId;
    }

    public String getLinkedClaimId() {
        return linkedClaimId;
    }

    public void setLinkedClaimId(String linkedClaimId) {
        this.linkedClaimId = linkedClaimId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}