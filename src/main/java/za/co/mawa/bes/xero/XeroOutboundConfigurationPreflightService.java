package za.co.mawa.bes.xero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local-only configuration preflight. Never calls Xero or changes integration state. */
@Service
public class XeroOutboundConfigurationPreflightService {
    private final JdbcTemplate jdbc;
    private final XeroIntegrationSettingsService settings;
    private final XeroActivationService activation;

    public XeroOutboundConfigurationPreflightService(JdbcTemplate jdbc,
                                                     XeroIntegrationSettingsService settings,
                                                     XeroActivationService activation) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.activation = activation;
    }

    public Map<String, Object> assess() {
        assertCanCheck();
        var configuration = activation.secretNames();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String organisationId = configuration.getSelectedTenantId();
        if (blank(organisationId)) blockers.add("A Xero accounting organisation must be selected");
        String accountCode = settings.invoiceAccountCode();
        if (blank(accountCode)) blockers.add("Invoice revenue account code is missing");
        if ("200".equals(accountCode)) warnings.add("Invoice revenue account code uses the default 200; verify in Xero");
        String taxType = settings.invoiceTaxType();
        if (blank(taxType)) blockers.add("Invoice tax type is missing");
        if ("NONE".equalsIgnoreCase(taxType)) warnings.add("Invoice tax type is NONE; confirm VAT treatment for this tenant");
        String paymentAccount = settings.paymentAccountCode();
        if (blank(paymentAccount)) blockers.add("Payment account code is missing");
        String amountType = settings.lineAmountTypes();
        if (!"Exclusive".equalsIgnoreCase(amountType) && !"Inclusive".equalsIgnoreCase(amountType)
                && !"NoTax".equalsIgnoreCase(amountType)) {
            blockers.add("Line amount type must be Exclusive, Inclusive or NoTax");
        }
        if (Boolean.TRUE.equals(configuration.getInvoiceIntegrationEnabled())
                || Boolean.TRUE.equals(configuration.getInvoiceIntegrationRequested())) {
            blockers.add("Invoice integration is enabled or requested; disable it during preflight");
        }
        blockers.add("Xero organisation, account codes, tax types and bank account have not been verified against the external ledger");
        blockers.add("Forward-only activation cutoff and initial posting selection require explicit approval");

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("invoiceAccountCode", accountCode);
        mapping.put("invoiceTaxType", taxType);
        mapping.put("lineAmountTypes", amountType);
        mapping.put("paymentAccountCode", paymentAccount);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "LOCAL_CONFIGURATION_READ_ONLY");
        result.put("mawaTenant", TenantContext.getCurrentTenant());
        result.put("selectedXeroTenantId", organisationId);
        result.put("integrationStatus", configuration.getIntegrationStatus());
        result.put("invoiceIntegrationEnabled", configuration.getInvoiceIntegrationEnabled());
        result.put("invoiceIntegrationRequested", configuration.getInvoiceIntegrationRequested());
        result.put("mapping", mapping);
        result.put("externalLedgerVerified", false);
        result.put("releaseAllowed", false);
        result.put("warnings", warnings);
        result.put("blockers", blockers);
        return result;
    }

    private void assertCanCheck() {
        String userId = UserContext.getCurrentUserId();
        if (blank(userId)) throw new AccessDeniedException("Authenticated ERP user required");
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_role ur
                JOIN `role` r ON r.id = ur.role
                JOIN `user` u ON u.id = ur.user
                JOIN xero_reconciliation_role_permission xp ON xp.role_id = r.id
                WHERE ur.user = ? AND xp.can_check = 1
                  AND u.status = 'ACTIVE'
                  AND (u.expires_at IS NULL OR u.expires_at > NOW())
                  AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                  AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                """, Integer.class, userId);
        if (count == null || count < 1) throw new AccessDeniedException("Insufficient Xero reconciliation permission");
    }

    private static boolean blank(String v) { return v == null || v.isBlank(); }
}
