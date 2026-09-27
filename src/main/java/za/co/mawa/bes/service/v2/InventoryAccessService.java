package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.access.UserAccessProfileDto;
import za.co.mawa.bes.service.UserAccessService;

import java.util.List;

@Service
public class InventoryAccessService {
    private final UserAccessService userAccessService;
    private final JdbcTemplate jdbcTemplate;

    public InventoryAccessService(UserAccessService userAccessService, JdbcTemplate jdbcTemplate) {
        this.userAccessService = userAccessService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String actorId() {
        String actor = UserContext.getCurrentUserId();
        if (StringUtils.hasText(actor)) return actor;
        UserAccessProfileDto profile = userAccessService.profile();
        if (StringUtils.hasText(profile.getUserId())) return profile.getUserId();
        throw new SecurityException("Authenticated user id is required for inventory operations");
    }

    public void requireWorkcentre(String workcentre) {
        userAccessService.assertWorkcentreAccess(workcentre, null);
    }

    public void requireAnyInventoryWorkcentre() {
        List<String> workcentres = List.of(
                "inventory-dashboard","stock-on-hand","goods-receipt","quality-inspection","putaway",
                "stock-replenishment","stock-transfer","warehouse-transfer","stock-reservation","stock-picking",
                "stock-dispatch","stock-returns","stock-count","stock-adjustment","stock-writeoff","stock-reversal",
                "stock-movement","inventory-audit","inventory-setup"
        );
        for (String workcentre : workcentres) {
            if (userAccessService.hasWorkcentreAccess(workcentre, null)) return;
        }
        throw new SecurityException("You do not have access to inventory operations");
    }

    public List<String> scopedWarehouseIds() {
        UserAccessProfileDto profile = userAccessService.profile();
        if (Boolean.TRUE.equals(profile.getAllWorkcentres())) return List.of();
        String userId = actorId();
        java.util.LinkedHashSet<String> scoped = new java.util.LinkedHashSet<>();
        scoped.addAll(jdbcTemplate.query("SELECT warehouse_id FROM user_warehouse_access WHERE user_id=?", (rs,rowNum) -> rs.getString(1), userId));
        List<String> roles = profile.getRoles() == null ? List.of() : profile.getRoles();
        for (String role : roles) {
            scoped.addAll(jdbcTemplate.query("SELECT warehouse_id FROM role_warehouse_access WHERE role_id=?", (rs,rowNum) -> rs.getString(1), role));
        }
        return List.copyOf(scoped);
    }

    public boolean canAccessWarehouse(String warehouseId) {
        if (!StringUtils.hasText(warehouseId)) return false;
        List<String> scope = scopedWarehouseIds();
        return scope.isEmpty() || scope.contains(warehouseId);
    }

    public void requireAnyWarehouse(String... warehouseIds) {
        List<String> scope = scopedWarehouseIds();
        if (scope.isEmpty()) return;
        if (warehouseIds != null) {
            for (String warehouseId : warehouseIds) if (StringUtils.hasText(warehouseId) && scope.contains(warehouseId)) return;
        }
        throw new SecurityException("You do not have access to this inventory document warehouse");
    }

    public void requireWarehouse(String warehouseId) {
        if (!StringUtils.hasText(warehouseId)) return;
        List<String> scope = scopedWarehouseIds();
        // No configured scope preserves backward-compatible unrestricted access.
        if (scope.isEmpty() || scope.contains(warehouseId)) return;
        throw new SecurityException("You do not have access to the selected warehouse");
    }
}
