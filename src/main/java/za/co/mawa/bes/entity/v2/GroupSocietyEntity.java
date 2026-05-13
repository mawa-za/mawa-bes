package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "group_society")
public class GroupSocietyEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private String partnerId;

    @Column(name = "group_no", nullable = false, unique = true)
    private String groupNo;

    @Column(name = "society_type")
    private String societyType;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "available_balance_cents", nullable = false)
    private Long availableBalanceCents = 0L;

    @Column(name = "total_paid_cents", nullable = false)
    private Long totalPaidCents = 0L;

    @Column(name = "total_claimed_cents", nullable = false)
    private Long totalClaimedCents = 0L;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "last_claim_date")
    private LocalDate lastClaimDate;

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

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public String getSocietyType() {
        return societyType;
    }

    public void setSocietyType(String societyType) {
        this.societyType = societyType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAvailableBalanceCents() {
        return availableBalanceCents;
    }

    public void setAvailableBalanceCents(Long availableBalanceCents) {
        this.availableBalanceCents = availableBalanceCents;
    }

    public Long getTotalPaidCents() {
        return totalPaidCents;
    }

    public void setTotalPaidCents(Long totalPaidCents) {
        this.totalPaidCents = totalPaidCents;
    }

    public Long getTotalClaimedCents() {
        return totalClaimedCents;
    }

    public void setTotalClaimedCents(Long totalClaimedCents) {
        this.totalClaimedCents = totalClaimedCents;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public LocalDate getLastClaimDate() {
        return lastClaimDate;
    }

    public void setLastClaimDate(LocalDate lastClaimDate) {
        this.lastClaimDate = lastClaimDate;
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