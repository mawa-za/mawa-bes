package za.co.mawa.bes.dto.moduleusage;

import java.time.LocalDateTime;

public class ModuleUsageResponse {

    private String id;
    private String userId;
    private String moduleCode;
    private String moduleName;
    private String modulePath;
    private String workcenterId;
    private Long usageCount;
    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;

    public ModuleUsageResponse() {
    }

    public ModuleUsageResponse(
            String id,
            String userId,
            String moduleCode,
            String moduleName,
            String modulePath,
            String workcenterId,
            Long usageCount,
            LocalDateTime firstUsedAt,
            LocalDateTime lastUsedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.modulePath = modulePath;
        this.workcenterId = workcenterId;
        this.usageCount = usageCount;
        this.firstUsedAt = firstUsedAt;
        this.lastUsedAt = lastUsedAt;
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
}
