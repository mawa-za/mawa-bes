package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimTypeConfigurationService {
    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("SELECT claim_type,enabled,display_order,updated_at,updated_by FROM claim_type_configuration ORDER BY display_order,claim_type");
    }

    public List<Map<String, Object>> enabled() {
        return jdbcTemplate.queryForList("SELECT claim_type,enabled,display_order FROM claim_type_configuration WHERE enabled=1 ORDER BY display_order,claim_type");
    }

    @Transactional
    public List<Map<String, Object>> save(List<Map<String, Object>> rows, String userId) {
        if (rows == null) throw new IllegalArgumentException("Claim type configuration is required");
        for (Map<String, Object> row : rows) {
            MembershipClaimType type = MembershipClaimType.valueOf(String.valueOf(row.get("claimType")).trim().toUpperCase());
            boolean enabled = row.get("enabled") != null && Boolean.parseBoolean(row.get("enabled").toString());
            int order = row.get("displayOrder") == null ? 100 : Integer.parseInt(row.get("displayOrder").toString());
            jdbcTemplate.update("""
                INSERT INTO claim_type_configuration(claim_type,enabled,display_order,updated_by)
                VALUES(?,?,?,?)
                ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),display_order=VALUES(display_order),updated_by=VALUES(updated_by)
                """, type.name(), enabled, order, userId);
        }
        return list();
    }

    public void requireEnabled(MembershipClaimType type) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM claim_type_configuration WHERE claim_type=? AND enabled=1",
                Integer.class, type.name());
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Claim type " + type.name() + " is not enabled in Claim Type Configuration");
        }
    }
}
