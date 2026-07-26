package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MembershipPolicyConfigurationService {
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> get() {
        var rows = jdbcTemplate.queryForList("SELECT * FROM membership_policy_configuration WHERE id='DEFAULT'");
        if (rows.isEmpty()) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("id", "DEFAULT");
            defaults.put("allow_multiple_memberships", false);
            defaults.put("additional_membership_requires_approval", true);
            return defaults;
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> request, String userId) {
        boolean allow = bool(request.get("allowMultipleMemberships"), false);
        boolean approval = bool(request.get("additionalMembershipRequiresApproval"), true);
        jdbcTemplate.update("""
            INSERT INTO membership_policy_configuration(id,allow_multiple_memberships,additional_membership_requires_approval,updated_by)
            VALUES('DEFAULT',?,?,?)
            ON DUPLICATE KEY UPDATE allow_multiple_memberships=VALUES(allow_multiple_memberships),additional_membership_requires_approval=VALUES(additional_membership_requires_approval),updated_by=VALUES(updated_by)
            """, allow, approval, userId);
        return get();
    }

    public boolean allowMultipleMemberships() { return bool(get().get("allow_multiple_memberships"), false); }
    public boolean additionalMembershipRequiresApproval() { return bool(get().get("additional_membership_requires_approval"), true); }

    private static boolean bool(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        String text = value.toString().trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
    }
}
