package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentAccountConfigurationService {
    private static final Set<String> ACCOUNT_ROLES = Set.of(
        "DEBTOR", "PETTY_CASH_CREDITOR", "CASH_CLAIM_CREDITOR"
    );

    private final JdbcTemplate jdbc;
    private final ReferenceDataValidationService referenceDataValidationService;

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("""
            SELECT *
              FROM payment_bank_account
             ORDER BY account_role, request_type, bank_name, account_holder
            """);
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> request) {
        String id = Objects.toString(request.get("id"), "").trim();
        if (id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        String role = required(request, "accountRole").toUpperCase();
        if (!ACCOUNT_ROLES.contains(role)) {
            throw new IllegalArgumentException("Unsupported payment account role: " + role);
        }

        String requestType = text(request.get("requestType"));
        if ("DEBTOR".equals(role)) {
            requestType = referenceDataValidationService.requireOption(
                "PAYMENT-REQUEST-TYPE", requestType, "Payment request type"
            );
        } else {
            requestType = null;
        }

        String bankName = referenceDataValidationService.requireOption(
            "BANK-NAME", required(request, "bankName"), "Bank name"
        );
        String accountType = referenceDataValidationService.requireOption(
            "BANK-ACCOUNT-TYPE", required(request, "accountType"), "Bank account type"
        );
        String accountHolder = required(request, "accountHolder");
        String accountNumber = required(request, "accountNumber");
        if (!accountNumber.matches("\\d{5,20}")) {
            throw new IllegalArgumentException("Bank account number must contain 5 to 20 numeric digits");
        }
        String branchCode = required(request, "branchCode");
        if (!branchCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("Branch code must contain exactly 6 numeric digits");
        }

        String bankIntegration = text(request.get("bankIntegration"));
        if (bankIntegration != null) {
            bankIntegration = bankIntegration.toUpperCase();
        }
        boolean active = booleanValue(request.get("active"), true);

        if (active) {
            jdbc.update("""
                UPDATE payment_bank_account
                   SET active = 0
                 WHERE account_role = ?
                   AND ((request_type IS NULL AND ? IS NULL) OR request_type = ?)
                   AND id <> ?
                """, role, requestType, requestType, id);
        }

        jdbc.update("""
            INSERT INTO payment_bank_account(
                id, account_role, request_type, bank_integration, bank_name,
                account_holder, account_number, branch_code, account_type,
                partner_id, active
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                account_role = VALUES(account_role),
                request_type = VALUES(request_type),
                bank_integration = VALUES(bank_integration),
                bank_name = VALUES(bank_name),
                account_holder = VALUES(account_holder),
                account_number = VALUES(account_number),
                branch_code = VALUES(branch_code),
                account_type = VALUES(account_type),
                partner_id = VALUES(partner_id),
                active = VALUES(active)
            """,
            id, role, requestType, bankIntegration, bankName, accountHolder,
            accountNumber, branchCode, accountType, text(request.get("partnerId")), active
        );
        return jdbc.queryForMap("SELECT * FROM payment_bank_account WHERE id = ?", id);
    }

    @Transactional
    public void deactivate(String id) {
        jdbc.update("UPDATE payment_bank_account SET active = 0 WHERE id = ?", id);
    }

    public Optional<Map<String, Object>> activeDebtor(String requestType) {
        return first("""
            SELECT *
              FROM payment_bank_account
             WHERE account_role = 'DEBTOR'
               AND request_type = ?
               AND active = 1
             ORDER BY updated_at DESC
             LIMIT 1
            """, requestType);
    }

    public Optional<Map<String, Object>> activeCreditor(String role) {
        return first("""
            SELECT *
              FROM payment_bank_account
             WHERE account_role = ?
               AND active = 1
             ORDER BY updated_at DESC
             LIMIT 1
            """, role);
    }

    public Optional<Map<String, Object>> activeFnbDebtor() {
        return jdbc.queryForList("""
            SELECT *
              FROM payment_bank_account
             WHERE account_role = 'DEBTOR'
               AND UPPER(COALESCE(bank_integration, '')) = 'FNB'
               AND active = 1
             ORDER BY updated_at DESC
             LIMIT 1
            """).stream().findFirst();
    }

    public boolean hasActiveFnbDebtor() {
        return activeFnbDebtor().isPresent();
    }

    private Optional<Map<String, Object>> first(String sql, Object... args) {
        return jdbc.queryForList(sql, args).stream().findFirst();
    }

    private static String required(Map<String, Object> values, String key) {
        String value = text(values.get(key));
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}
