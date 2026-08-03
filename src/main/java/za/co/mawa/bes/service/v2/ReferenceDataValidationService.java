package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReferenceDataValidationService {
    private final JdbcTemplate jdbcTemplate;

    public String requireOption(String field, String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
        String normalized = value.trim();
        var options = jdbcTemplate.queryForList("""
            SELECT code
              FROM field_option
             WHERE field = ?
               AND (UPPER(code) = UPPER(?) OR UPPER(description) = UPPER(?))
               AND (valid_from IS NULL OR valid_from <= CURRENT_DATE)
               AND (valid_to IS NULL OR valid_to >= CURRENT_DATE)
             ORDER BY CASE WHEN UPPER(code) = UPPER(?) THEN 0 ELSE 1 END
             LIMIT 1
            """, String.class, field, normalized, normalized, normalized);
        if (options.isEmpty()) {
            throw new IllegalArgumentException(label + " must be selected from " + field);
        }
        return options.get(0);
    }

    public String optionalOption(String field, String value, String label) {
        return StringUtils.hasText(value) ? requireOption(field, value, label) : null;
    }

    public String description(String field, String code) {
        if (!StringUtils.hasText(field) || !StringUtils.hasText(code)) return null;
        var descriptions = jdbcTemplate.queryForList("""
            SELECT description
              FROM field_option
             WHERE field = ?
               AND UPPER(code) = UPPER(?)
               AND (valid_from IS NULL OR valid_from <= CURRENT_DATE)
               AND (valid_to IS NULL OR valid_to >= CURRENT_DATE)
             ORDER BY type DESC
             LIMIT 1
            """, String.class, field.trim(), code.trim());
        return descriptions.isEmpty() ? null : descriptions.get(0);
    }

    public String requireContactNumber(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Contact number is required");
        }

        String normalized = value.trim().replaceAll("[\\s()\\-]", "");
        if (normalized.startsWith("+27")) {
            normalized = "0" + normalized.substring(3);
        } else if (normalized.startsWith("27") && normalized.length() == 11) {
            normalized = "0" + normalized.substring(2);
        }

        if (!normalized.matches("0\\d{9}")) {
            throw new IllegalArgumentException("Contact number must be a valid 10-digit South African number");
        }
        return normalized;
    }

    public String optionalContactNumber(String value) {
        return StringUtils.hasText(value) ? requireContactNumber(value) : null;
    }
}
