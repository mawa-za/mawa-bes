package za.co.mawa.bes.dto.moduleusage;

import jakarta.validation.constraints.NotBlank;

public class TrackModuleUsageRequest {

    /**
     * Optional. If blank, API will use the authenticated principal name.
     */
    private String userId;

    @NotBlank(message = "moduleCode is required")
    private String moduleCode;

    private String moduleName;
    private String modulePath;
    private String workcenterId;

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
}
