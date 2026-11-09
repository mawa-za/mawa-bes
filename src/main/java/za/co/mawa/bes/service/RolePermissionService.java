package za.co.mawa.bes.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Distinct business-action access controls. Never use role_workcenter for actions. */
@Service
public class RolePermissionService {
    private final JdbcTemplate jdbc;
    private final RoleRepository roles;
    private final UserAccessService access;

    public RolePermissionService(JdbcTemplate jdbc, RoleRepository roles, UserAccessService access) {
        this.jdbc = jdbc;
        this.roles = roles;
        this.access = access;
    }

    public List<Map<String, Object>> catalogue() {
        return jdbc.queryForList("""
                SELECT code, description, details, group_code AS groupCode,
                       group_title AS groupTitle, display_order AS displayOrder,
                       sensitive
                FROM permission_catalog WHERE active = TRUE
                ORDER BY group_title, display_order, description
                """);
    }

    public List<String> assigned(String roleId) {
        requireRole(roleId);
        return jdbc.queryForList("SELECT permission_code FROM role_permission WHERE role_id = ? ORDER BY permission_code",
                String.class, roleId);
    }

    /** Atomic replacement avoids half-applied role permissions. Protected-admin only. */
    @Transactional
    public List<String> replace(String roleId, List<String> requested) {
        if (!access.isProtectedAdministrator()) {
            throw new SecurityException("Only a protected tenant administrator can manage business-action permissions");
        }
        requireRole(roleId);
        if (requested == null) throw new IllegalArgumentException("Permissions list is required");
        Set<String> desired = new LinkedHashSet<>();
        for (String code : requested) {
            if (!StringUtils.hasText(code)) throw new IllegalArgumentException("Permission code is required");
            desired.add(code.trim());
        }
        Set<String> valid = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT code FROM permission_catalog WHERE active = TRUE", String.class));
        if (!valid.containsAll(desired)) throw new IllegalArgumentException("Unknown or inactive permission code");
        Set<String> existing = new LinkedHashSet<>(assigned(roleId));
        String actor = StringUtils.hasText(UserContext.getCurrentUserId()) ? UserContext.getCurrentUserId()
                : StringUtils.hasText(UserContext.getCurrentUser()) ? UserContext.getCurrentUser() : "SYSTEM";
        for (String code : existing) {
            if (!desired.contains(code)) {
                jdbc.update("DELETE FROM role_permission WHERE role_id = ? AND permission_code = ?", roleId, code);
                audit(roleId, code, "REVOKE", actor);
            }
        }
        for (String code : desired) {
            if (!existing.contains(code)) {
                jdbc.update("INSERT INTO role_permission(role_id, permission_code) VALUES (?, ?)", roleId, code);
                audit(roleId, code, "GRANT", actor);
            }
        }
        return assigned(roleId);
    }

    private void audit(String roleId, String code, String action, String actor) {
        jdbc.update("INSERT INTO role_permission_audit(role_id, permission_code, action, changed_by) VALUES (?, ?, ?, ?)",
                roleId, code, action, actor);
    }

    private void requireRole(String roleId) {
        if (!StringUtils.hasText(roleId) || !roles.existsById(roleId)) {
            throw new IllegalArgumentException("Role not found");
        }
    }
}
