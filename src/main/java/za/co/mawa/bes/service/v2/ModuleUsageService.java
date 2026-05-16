package za.co.mawa.bes.service.v2;

import za.co.mawa.bes.dto.moduleusage.ModuleUsageResponse;
import za.co.mawa.bes.dto.moduleusage.TrackModuleUsageRequest;

import java.util.List;

public interface ModuleUsageService {

    ModuleUsageResponse trackUsage(TrackModuleUsageRequest request);

    List<ModuleUsageResponse> getFrequentlyUsedModules(String userId, int limit);

    List<ModuleUsageResponse> getRecentlyUsedModules(String userId, int limit);

    void resetUserUsage(String userId);
}
