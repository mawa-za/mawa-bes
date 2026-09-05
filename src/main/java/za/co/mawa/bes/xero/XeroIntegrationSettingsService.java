package za.co.mawa.bes.xero;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

@Service
public class XeroIntegrationSettingsService {

    public static final String XERO_SETTINGS_GROUP = "XERO";
    public static final String PROP_INVOICE_ENABLED = "XERO-INVOICE-INTEGRATION-ENABLED";
    public static final String PROP_INTEGRATION_ENABLED = "XERO-INTEGRATION-ENABLED";
    public static final String PROP_INVOICE_STATUS = "XERO-INVOICE-STATUS";
    public static final String PROP_INVOICE_ACCOUNT_CODE = "XERO-INVOICE-ACCOUNT-CODE";
    public static final String PROP_INVOICE_TAX_TYPE = "XERO-INVOICE-TAX-TYPE";
    public static final String PROP_LINE_AMOUNT_TYPES = "XERO-LINE-AMOUNT-TYPES";

    @Autowired
    private TenantAdminService tenantAdminService;

    @Autowired
    private SettingService settingService;

    public boolean isInvoiceIntegrationEnabled() {
        JSONObject properties = currentTenantProperties();

        String value = firstNonBlank(
                properties.optString(PROP_INVOICE_ENABLED, null),
                properties.optString(PROP_INTEGRATION_ENABLED, null),
                settingService.getSetting("INVOICE-INTEGRATION", XERO_SETTINGS_GROUP),
                settingService.getSetting("INTEGRATION", XERO_SETTINGS_GROUP)
        );

        return isTruthy(value);
    }

    public boolean isIntegrationEnabled() {
        JSONObject properties = currentTenantProperties();
        return isTruthy(firstNonBlank(
                properties.optString(PROP_INTEGRATION_ENABLED, null),
                properties.optString(PROP_INVOICE_ENABLED, null),
                settingService.getSetting("INTEGRATION", XERO_SETTINGS_GROUP),
                settingService.getSetting("INVOICE-INTEGRATION", XERO_SETTINGS_GROUP)));
    }

    public String invoiceStatus() {
        JSONObject properties = currentTenantProperties();
        String status = firstNonBlank(
                properties.optString(PROP_INVOICE_STATUS, null),
                settingService.getSetting("INVOICE-STATUS", XERO_SETTINGS_GROUP)
        );
        if (isBlank(status)) {
            return "DRAFT";
        }
        return status.trim().toUpperCase();
    }

    public String invoiceAccountCode() {
        JSONObject properties = currentTenantProperties();
        return firstNonBlank(
                properties.optString(PROP_INVOICE_ACCOUNT_CODE, null),
                settingService.getSetting("INVOICE-ACCOUNT-CODE", XERO_SETTINGS_GROUP),
                settingService.getSetting("ACCOUNT-CODE", XERO_SETTINGS_GROUP),
                "200"
        );
    }

    public String invoiceTaxType() {
        JSONObject properties = currentTenantProperties();
        return firstNonBlank(
                properties.optString(PROP_INVOICE_TAX_TYPE, null),
                settingService.getSetting("INVOICE-TAX-TYPE", XERO_SETTINGS_GROUP),
                "NONE"
        );
    }

    public String lineAmountTypes() {
        JSONObject properties = currentTenantProperties();
        return firstNonBlank(
                properties.optString(PROP_LINE_AMOUNT_TYPES, null),
                settingService.getSetting("LINE-AMOUNT-TYPES", XERO_SETTINGS_GROUP),
                "Exclusive"
        );
    }

    private JSONObject currentTenantProperties() {
        String tenant = TenantContext.getCurrentTenant();
        if (isBlank(tenant)) {
            return new JSONObject();
        }
        try {
            String tenantProperty = tenantAdminService.getTenantProperty(tenant);
            if (isBlank(tenantProperty)) {
                return new JSONObject();
            }
            return new JSONObject(tenantProperty);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private boolean isTruthy(String value) {
        if (isBlank(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        return "1".equals(normalized)
                || "Y".equals(normalized)
                || "YES".equals(normalized)
                || "TRUE".equals(normalized)
                || "ENABLED".equals(normalized)
                || "ON".equals(normalized);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }
}
