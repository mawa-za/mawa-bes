package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UniversalBranchCodeService {
    private final JdbcTemplate jdbcTemplate;
    private final ReferenceDataValidationService referenceDataValidationService;

    public String resolve(String bankName) {
        String bankCode = referenceDataValidationService.requireOption(
                "BANK-NAME", bankName, "Bank name");
        List<String> values = jdbcTemplate.query(
                "SELECT universal_branch_code FROM bank_universal_branch_code WHERE bank_code = ? AND active = 1",
                (rs, rowNum) -> rs.getString(1), bankCode);
        if (values.isEmpty() || values.get(0) == null || !values.get(0).matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "No universal branch code is configured for bank " + bankCode);
        }
        return values.get(0);
    }

    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("""
                SELECT bank_code AS bankCode,
                       universal_branch_code AS universalBranchCode,
                       description,
                       active
                  FROM bank_universal_branch_code
                 ORDER BY bank_code
                """);
    }
}
