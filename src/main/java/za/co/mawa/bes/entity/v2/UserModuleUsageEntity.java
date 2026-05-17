package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_module_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_module_usage_user_module",
                        columnNames = {"user_id", "module_code"}
                )
        }
)
public class UserModuleUsageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(name = "module_name", length = 150)
    private String moduleName;

    @Column(name = "module_path")
    private String modulePath;

    @Column(name = "workcenter_id")
    private String workcenterId;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Column(name = "first_used_at")
    private LocalDateTime firstUsedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (usageCount == null) {
            usageCount = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void increaseUsage(LocalDateTime usedAt) {
        if (usageCount == null) {
            usageCount = 0L;
        }

        usageCount++;

        if (firstUsedAt == null) {
            firstUsedAt = usedAt;
        }

        lastUsedAt = usedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getModulePath() { return modulePath; }
    public void setModulePath(String modulePath) { this.modulePath = modulePath; }

    public String getWorkcenterId() { return workcenterId; }
    public void setWorkcenterId(String workcenterId) { this.workcenterId = workcenterId; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public LocalDateTime getFirstUsedAt() { return firstUsedAt; }
    public void setFirstUsedAt(LocalDateTime firstUsedAt) { this.firstUsedAt = firstUsedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
