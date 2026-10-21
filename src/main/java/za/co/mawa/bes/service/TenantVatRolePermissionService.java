package za.co.mawa.bes.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;

/** Admin-managed, tenant-local VAT configuration permissions. No Xero dependency. */
@Service
public class TenantVatRolePermissionService {
    private final JdbcTemplate jdbc;
    private final UserAccessService access;

    public TenantVatRolePermissionService(JdbcTemplate jdbc, UserAccessService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    public record UpdateRequest(Boolean canView, Boolean canManage) { }

    public Map<String, Object> get(String roleId) {
        requireProtectedAdmin();
        requireRole(roleId);
        var rows = jdbc.queryForList("""
            SELECT can_view, can_manage FROM tenant_vat_role_permission WHERE role_id = ?
            """, roleId);
        var result = new LinkedHashMap<String, Object>();
        result.put("roleId", roleId);
        result.put("canView", !rows.isEmpty() && ((Number) rows.get(0).get("can_view")).intValue() == 1);
        result.put("canManage", !rows.isEmpty() && ((Number) rows.get(0).get("can_manage")).intValue() == 1);
        return result;
    }

    @Transactional
    public Map<String, Object> update(String roleId, UpdateRequest request) {
        requireProtectedAdmin();
        requireRole(roleId);
        if (request == null || request.canView() == null || request.canManage() == null) {
            throw new IllegalArgumentException("Both VAT permissions must be explicitly provided");
        }
        if (request.canManage() && !request.canView()) {
            throw new IllegalArgumentException("Manage VAT configuration requires view permission");
        }
        jdbc.update("""
            INSERT INTO tenant_vat_role_permission(role_id, can_view, can_manage)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE can_view = VALUES(can_view), can_manage = VALUES(can_manage)
            """, roleId, request.canView(), request.canManage());
        return get(roleId);
    }

    private void requireRole(String roleId) {
        if (roleId == null || roleId.isBlank()) throw new IllegalArgumentException("Role is required");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `role` WHERE id = ?", Integer.class, roleId);
        if (count == null || count == 0) throw new IllegalArgumentException("Role does not exist");
    }

    private void requireProtectedAdmin() {
        if (UserContext.getCurrentUserId() == null || !access.isProtectedAdministrator()) {
            throw new AccessDeniedException("Protected tenant administrator required to assign VAT permissions");
        }
    }
}
