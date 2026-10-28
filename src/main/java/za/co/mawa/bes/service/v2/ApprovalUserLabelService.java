package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalUserLabelService {
    private final JdbcTemplate jdbcTemplate;

    public String displayName(String identity) {
        if (identity == null || identity.isBlank()) return "Unknown user";
        List<String> names = jdbcTemplate.queryForList("""
            SELECT COALESCE(NULLIF(TRIM(username), ''), NULLIF(TRIM(email), ''), 'Unknown user')
              FROM `user`
             WHERE id = ? OR username = ? OR email = ?
             ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END LIMIT 1
            """, String.class, identity, identity, identity, identity);
        // Do not leak technical database identifiers into user-facing responses.
        return names.isEmpty() ? "Unknown user" : names.get(0);
    }
}
