package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.UserAccessService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only Xero chart/tax lookup and explicit, organisation-bound local configuration. */
@Service
public class XeroAccountingConfigurationService {
    private static final String GROUP = "XERO";
    private static final String API = "https://api.xero.com/api.xro/2.0/";
    private static final Set<String> REVENUE_TYPES = Set.of("REVENUE", "SALES", "OTHERINCOME");
    private final XeroAuthService auth;
    private final XeroIntegrationSettingsService settings;
    private final SettingService store;
    private final UserAccessService access;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final java.util.concurrent.ConcurrentMap<String, CachedValidation> validations = new java.util.concurrent.ConcurrentHashMap<>();
    private record CachedValidation(XeroAccountingMapping mapping, long validUntilNanos, boolean valid) { }


    public XeroAccountingConfigurationService(XeroAuthService auth, XeroIntegrationSettingsService settings,
                                              SettingService store, UserAccessService access, JdbcTemplate jdbc) {
        this.auth = auth;
        this.settings = settings;
        this.store = store;
        this.access = access;
        this.jdbc = jdbc;
    }

    public record AccountOption(String accountId, String code, String name, String type, String taxType) { }
    public record TaxOption(String taxType, String name, java.math.BigDecimal displayRate) { }
    public record Options(String organisationId, List<AccountOption> invoiceAccounts,
                          List<AccountOption> paymentAccounts, List<TaxOption> taxRates) { }
    public record Configuration(Options options, XeroAccountingMapping mapping,
                                String legacyInvoiceAccountCode, String legacyPaymentAccountCode,
                                String legacyInvoiceTaxType, boolean mappingValid, boolean invoiceIntegrationEnabled) { }
    public record SaveRequest(String organisationId, String invoiceAccountId, String paymentAccountId,
                              String invoiceTaxType) { }

    public void requireConfigurationAccess() {
        // Same tenant workcentre access as ERP Settings / Xero; no external or platform credentials exposed.
        if (!access.hasWorkcentreAccess("system-configuration", null)
                && !access.hasWorkcentreAccess("system-configurations", null)
                && !access.hasWorkcentreAccess("xero-integration-admin", null)
                && !access.hasWorkcentreAccess("xero-integration", null)
                && !access.hasWorkcentreAccess("xero", null)) {
            throw new AccessDeniedException("You do not have access to Xero configuration");
        }
    }

    public Configuration read() {
        requireConfigurationAccess();
        Options options = loadOptions();
        Configuration result = configuration(options);
        rememberValidation(result.mapping(), result.mappingValid());
        return result;
    }

    public void requireConfigurationWriteAccess() {
        requireConfigurationAccess();
        access.assertExternalTransactionAllowed("XERO-ACCOUNTING-CONFIGURATION");
    }

    public void rememberOrganisationBeforeConnectionChange() {
        XeroAccountingMapping mapping = settings.accountingMapping();
        if (mapping != null || store.getSetting("ACCOUNTING-PREVIOUS-ORGANISATION", GROUP) != null) return;
        if (!hasLinkedRecords()) return;
        String tenant = TenantContext.getCurrentTenant();
        String previous = auth.getXeroProperty(tenant, XeroUtils.XERO_TENANT_ID);
        if (previous == null || previous.isBlank()) {
            throw new IllegalStateException("Existing Xero records have no identifiable organisation. Reconcile the records before reconnecting to another ledger.");
        }
        store.upsertSetting("ACCOUNTING-PREVIOUS-ORGANISATION", GROUP, previous);
    }

    public Configuration save(SaveRequest request) {
        requireConfigurationWriteAccess();
        if (settings.isInvoiceIntegrationEnabled() || settings.isIntegrationEnabled()) {
            throw new IllegalStateException("Disable invoice integration before changing Xero accounting configuration");
        }
        if (request == null) throw new IllegalArgumentException("Accounting configuration is required");
        Options options = loadOptions();
        XeroAccountingMapping mapping = validateSelection(options, request);
        // Re-check the selected organisation after external lookups, before writing the mapping.
        if (!mapping.matchesOrganisation(selectedOrganisation())) {
            throw new IllegalStateException("Xero organisation changed. Refresh configuration and try again.");
        }
        XeroAccountingMapping previous = settings.accountingMapping();
        // Never reinterpret already-linked financial records as belonging to a different ledger.
        String oldOrganisation = previous == null ? store.getSetting("ACCOUNTING-PREVIOUS-ORGANISATION", GROUP) : previous.organisationId();
        if (oldOrganisation != null && !oldOrganisation.isBlank()
                && !oldOrganisation.equalsIgnoreCase(mapping.organisationId()) && hasLinkedRecords()) {
            throw new IllegalStateException("Existing Xero records belong to another organisation. Reconcile or migrate them before changing the ledger.");
        }
        if (settings.isInvoiceIntegrationEnabled() || settings.isIntegrationEnabled()) {
            throw new IllegalStateException("Invoice integration was enabled while validating accounts. Disable it and retry.");
        }
        Integer possibleLeases = jdbc.queryForObject("""
                SELECT COUNT(*) FROM message_queue WHERE processed = FALSE
                  AND message_type IN ('XERO-INVOICE','XERO-CUSTOMER','XERO-PRODUCT')
                  AND next_attempt_at > NOW() AND next_attempt_at <= DATE_ADD(NOW(), INTERVAL 15 MINUTE)
                """, Integer.class);
        if (possibleLeases != null && possibleLeases > 0) {
            throw new IllegalStateException("Xero work is in flight or waiting to retry. Let the lease/retry interval expire before changing accounts.");
        }
        String value = mapping.serialise();
        if (value.length() > 1024) throw new IllegalArgumentException("Xero accounting configuration exceeds the storage limit");
        store.upsertSetting(XeroAccountingMapping.ATTRIBUTE, GROUP, value);
        rememberValidation(mapping, true);
        return configuration(options);
    }

    /** Revalidate live Xero values before enabling outbound integration. No mutation or posting. */
    public void validateForEnablement() {
        XeroAccountingMapping mapping = settings.accountingMapping();
        if (mapping == null) throw new IllegalStateException("Select and save the Xero invoice account, payment account and tax rate first");
        Options options = loadOptions();
        boolean valid = mappingValid(options, mapping);
        rememberValidation(mapping, valid);
        if (!valid) {
            throw new IllegalStateException("Saved Xero accounting configuration is no longer valid. Refresh and save the account selections.");
        }
    }

    /** A fresh lookup on save/enable, then at most every five minutes per tenant/instance during dispatch.
     *  The posting services still verify the exact organisation for every write. */
    public void validateForDispatch() {
        String tenant = TenantContext.getCurrentTenant();
        XeroAccountingMapping mapping = settings.accountingMapping();
        if (mapping == null || !mapping.matchesOrganisation(selectedOrganisation())) {
            throw new IllegalStateException("Xero accounting mapping is missing or belongs to another organisation");
        }
        CachedValidation cached = validations.get(tenant);
        if (cached != null && mapping.equals(cached.mapping()) && System.nanoTime() - cached.validUntilNanos() < 0) {
            if (!cached.valid()) throw new IllegalStateException("Saved Xero account or tax mapping is invalid");
            return;
        }
        try {
            validateForEnablement();
        } catch (RuntimeException invalid) {
            rememberValidation(mapping, false);
            throw invalid;
        }
    }

    private void rememberValidation(XeroAccountingMapping mapping, boolean valid) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) return;
        if (mapping == null) { validations.remove(tenant); return; }
        long duration = java.util.concurrent.TimeUnit.SECONDS.toNanos(valid ? 300 : 60);
        validations.put(tenant, new CachedValidation(mapping, System.nanoTime() + duration, valid));
    }

    private Configuration configuration(Options options) {
        XeroAccountingMapping mapping = settings.accountingMapping();
        return new Configuration(options, mapping, settings.invoiceAccountCode(), settings.paymentAccountCode(),
                settings.invoiceTaxType(), mappingValid(options, mapping), settings.isInvoiceIntegrationEnabled());
    }

    private String selectedOrganisation() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank() || !"AUTHORISED".equalsIgnoreCase(store.getSetting("INTEGRATION-STATUS", GROUP))) {
            throw new IllegalStateException("Authorise Xero and select an organisation first");
        }
        // Deliberately resolve the current MAWA tenant only. Do not fall back to another tenant's connection.
        String id = auth.getXeroProperty(tenant, XeroUtils.XERO_TENANT_ID);
        if (id == null || id.isBlank()) throw new IllegalStateException("Select a Xero organisation first");
        return id;
    }

    Options loadOptions() {
        String tenant = TenantContext.getCurrentTenant();
        String organisation = selectedOrganisation();
        try {
            String token = auth.refreshAccessToken(tenant);
            JsonNode accounts = get("Accounts", token, organisation).path("Accounts");
            JsonNode taxes = get("TaxRates", token, organisation).path("TaxRates");
            if (!accounts.isArray() || !taxes.isArray()) throw new IOException("Unexpected chart of accounts or tax rates response");
            return parseOptions(organisation, accounts, taxes);
        } catch (IOException e) {
            // Do not echo tokens, raw ledger payloads or Xero error bodies to ERP.
            throw new IllegalStateException("Unable to retrieve Xero accounts and tax rates. Check the connection and accounting.settings permission, then retry.", e);
        } finally {
            TenantContext.setCurrentTenant(tenant);
        }
    }

    private JsonNode get(String resource, String token, String organisation) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(API + resource).toURL().openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Xero-Tenant-Id", organisation);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("Xero " + resource + " lookup returned HTTP " + status);
            try (var input = connection.getInputStream()) { return mapper.readTree(input); }
        } finally {
            connection.disconnect();
        }
    }

    static Options parseOptions(String organisation, JsonNode accounts, JsonNode taxes) {
        List<AccountOption> invoices = new ArrayList<>();
        List<AccountOption> payments = new ArrayList<>();
        List<TaxOption> taxRates = new ArrayList<>();
        for (JsonNode account : accounts) {
            String id = account.path("AccountID").asText("");
            String code = account.path("Code").asText("");
            String type = account.path("Type").asText("").toUpperCase(Locale.ROOT);
            // The Accounts API returns Status on full account records. Fail closed if it is missing.
            if (id.isBlank() || code.isBlank() || !"ACTIVE".equalsIgnoreCase(account.path("Status").asText())) continue;
            AccountOption option = new AccountOption(id, code, account.path("Name").asText(code), type,
                    account.path("TaxType").asText(""));
            if (REVENUE_TYPES.contains(type) && account.path("SystemAccount").asText("").isBlank()) invoices.add(option);
            if ("BANK".equals(type) || account.path("EnablePaymentsToAccount").asBoolean(false)) payments.add(option);
        }
        for (JsonNode tax : taxes) {
            String type = tax.path("TaxType").asText("");
            if (type.isBlank() || !"ACTIVE".equalsIgnoreCase(tax.path("Status").asText())
                    || !tax.path("CanApplyToRevenue").asBoolean(false)
                    || "USSALESTAX".equalsIgnoreCase(tax.path("ReportTaxType").asText())) continue;
            taxRates.add(new TaxOption(type, tax.path("Name").asText(type), tax.path("DisplayTaxRate").decimalValue()));
        }
        Comparator<AccountOption> order = Comparator.comparing(AccountOption::code).thenComparing(AccountOption::name);
        invoices.sort(order);
        payments.sort(order);
        taxRates.sort(Comparator.comparing(TaxOption::name));
        return new Options(organisation, List.copyOf(invoices), List.copyOf(payments), List.copyOf(taxRates));
    }

    static XeroAccountingMapping validateSelection(Options options, SaveRequest request) {
        if (request.organisationId() == null || !options.organisationId().equalsIgnoreCase(request.organisationId())) {
            throw new IllegalArgumentException("The selected Xero organisation changed. Refresh the accounts.");
        }
        AccountOption invoice = options.invoiceAccounts().stream()
                .filter(a -> a.accountId().equals(request.invoiceAccountId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an active Xero revenue account"));
        AccountOption payment = options.paymentAccounts().stream()
                .filter(a -> a.accountId().equals(request.paymentAccountId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an active bank or payment-enabled Xero account"));
        TaxOption tax = options.taxRates().stream().filter(t -> t.taxType().equals(request.invoiceTaxType())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an active Xero tax rate compatible with revenue accounts"));
        return new XeroAccountingMapping(options.organisationId(), invoice.accountId(), invoice.code(),
                payment.accountId(), payment.code(), tax.taxType());
    }

    static boolean mappingValid(Options options, XeroAccountingMapping mapping) {
        if (mapping == null || !mapping.matchesOrganisation(options.organisationId())) return false;
        try {
            return mapping.equals(validateSelection(options, new SaveRequest(options.organisationId(),
                    mapping.invoiceAccountId(), mapping.paymentAccountId(), mapping.invoiceTaxType())));
        } catch (IllegalArgumentException invalid) { return false; }
    }

    private boolean hasLinkedRecords() {
        Integer linked = jdbc.queryForObject("""
                SELECT CASE WHEN EXISTS (SELECT 1 FROM invoice WHERE xero_invoice_id IS NOT NULL AND xero_invoice_id <> '')
                  OR EXISTS (SELECT 1 FROM partner WHERE xero_contact_id IS NOT NULL AND xero_contact_id <> '')
                  OR EXISTS (SELECT 1 FROM product WHERE xero_item_id IS NOT NULL AND xero_item_id <> '')
                  OR EXISTS (SELECT 1 FROM invoice_payment WHERE xero_payment_id IS NOT NULL AND xero_payment_id <> '')
                  OR EXISTS (SELECT 1 FROM credit_note WHERE xero_credit_note_id IS NOT NULL AND xero_credit_note_id <> '')
                  THEN 1 ELSE 0 END
                """, Integer.class);
        return linked == null || linked > 0;
    }
}
