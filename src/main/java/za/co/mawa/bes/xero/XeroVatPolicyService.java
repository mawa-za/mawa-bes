package za.co.mawa.bes.xero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;

/** Deliberately independent of Xero posting; policy history is append-only. */
@Service
public class XeroVatPolicyService {
    private final JdbcTemplate jdbc;
    public XeroVatPolicyService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record PolicyRequest(Boolean vatRegistered, String vatRegistrationNumber,
            String defaultPricingMethod, BigDecimal standardRatePercent,
            LocalDate effectiveFrom, LocalDate effectiveTo) { }

    public Map<String, Object> read() {
        requirePermission(false);
        var policies = jdbc.queryForList("""
            SELECT id, vat_registered, vat_registration_number, default_pricing_method,
                   standard_rate_percent, effective_from, effective_to, created_at, created_by
              FROM tenant_vat_policy ORDER BY effective_from DESC, created_at DESC
            """);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "VAT_POLICY_CONFIGURATION_ONLY");
        out.put("policies", policies);
        out.put("invoiceIntegrationChanged", false);
        out.put("releaseAllowed", false);
        return out;
    }

    /** The transaction's SERIALIZABLE isolation protects overlapping effective-date inserts. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Object> create(PolicyRequest request) {
        String userId = requirePermission(true);
        validate(request);
        Integer overlaps = jdbc.queryForObject("""
            SELECT COUNT(*) FROM tenant_vat_policy
             WHERE effective_from <= COALESCE(?, '9999-12-31')
               AND (effective_to IS NULL OR effective_to >= ?)
            """, Integer.class, request.effectiveTo(), request.effectiveFrom());
        if (overlaps == null || overlaps > 0) {
            throw new IllegalArgumentException("VAT policy effective dates overlap an existing policy");
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO tenant_vat_policy
              (id, vat_registered, vat_registration_number, default_pricing_method,
               standard_rate_percent, effective_from, effective_to, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, id, request.vatRegistered(), trimToNull(request.vatRegistrationNumber()),
                request.defaultPricingMethod(), request.standardRatePercent(),
                request.effectiveFrom(), request.effectiveTo(), userId);
        return Map.of("id", id, "created", true, "invoiceIntegrationChanged", false,
                "releaseAllowed", false);
    }

    static void validate(PolicyRequest r) {
        if (r == null || r.vatRegistered() == null || r.effectiveFrom() == null) {
            throw new IllegalArgumentException("VAT registration and effectiveFrom are required");
        }
        if (!"INCLUSIVE".equals(r.defaultPricingMethod()) && !"EXCLUSIVE".equals(r.defaultPricingMethod())) {
            throw new IllegalArgumentException("Pricing method must be INCLUSIVE or EXCLUSIVE");
        }
        if (r.effectiveFrom().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("VAT policy cannot take effect retrospectively");
        }
        if (r.effectiveTo() != null && r.effectiveTo().isBefore(r.effectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo precedes effectiveFrom");
        }
        if (r.vatRegistered() && trimToNull(r.vatRegistrationNumber()) == null) {
            throw new IllegalArgumentException("VAT registration number is required for VAT-registered tenants");
        }
        if (!r.vatRegistered() && trimToNull(r.vatRegistrationNumber()) != null) {
            throw new IllegalArgumentException("Non-VAT-registered tenants cannot set a VAT number");
        }
        if (r.standardRatePercent() != null && (r.standardRatePercent().compareTo(BigDecimal.ZERO) < 0
                || r.standardRatePercent().compareTo(new BigDecimal("100")) > 0
                || r.standardRatePercent().scale() > 4)) {
            throw new IllegalArgumentException("VAT rate must be between 0 and 100 with up to 4 decimals");
        }
        if (r.vatRegistered() && r.standardRatePercent() == null) {
            throw new IllegalArgumentException("Standard VAT rate is required for VAT-registered tenants");
        }
        if (!r.vatRegistered() && r.standardRatePercent() != null
                && r.standardRatePercent().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Non-VAT-registered tenants cannot charge a standard VAT rate");
        }
    }

    private String requirePermission(boolean manage) {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.isBlank()) throw new AccessDeniedException("Authenticated ERP user required");
        String permissionJoin = manage
                ? "JOIN tenant_vat_role_permission xp ON xp.role_id = r.id AND xp.can_manage = 1"
                : "JOIN tenant_vat_role_permission xp ON xp.role_id = r.id AND xp.can_view = 1";
        Integer allowed = jdbc.queryForObject("""
             SELECT COUNT(*) FROM user_role ur
             JOIN `role` r ON r.id = ur.role
             JOIN `user` u ON u.id = ur.user
             """ + permissionJoin + """
              WHERE ur.user = ? AND u.status = 'ACTIVE'
                AND (u.expires_at IS NULL OR u.expires_at > NOW())
                AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
            """, Integer.class, userId);
        if (allowed == null || allowed == 0) throw new AccessDeniedException("Insufficient tenant VAT configuration permission");
        return userId;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
