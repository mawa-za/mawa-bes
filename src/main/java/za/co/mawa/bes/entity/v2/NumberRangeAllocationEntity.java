package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "number_range_allocation")
public class NumberRangeAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seq_type", nullable = false, length = 64)
    private String seqType;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "from_no", nullable = false)
    private Long fromNo;

    @Column(name = "to_no", nullable = false)
    private Long toNo;

    @Column(name = "next_local_no", nullable = false)
    private Long nextLocalNo;

    @Column(name = "allocation_size", nullable = false)
    private Integer allocationSize;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public String getSeqType() {
        return seqType;
    }

    public void setSeqType(String seqType) {
        this.seqType = seqType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getFromNo() {
        return fromNo;
    }

    public void setFromNo(Long fromNo) {
        this.fromNo = fromNo;
    }

    public Long getToNo() {
        return toNo;
    }

    public void setToNo(Long toNo) {
        this.toNo = toNo;
    }

    public Long getNextLocalNo() {
        return nextLocalNo;
    }

    public void setNextLocalNo(Long nextLocalNo) {
        this.nextLocalNo = nextLocalNo;
    }

    public Integer getAllocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(Integer allocationSize) {
        this.allocationSize = allocationSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}