package za.co.mawa.bes.service.v2.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.dto.v2.integration.XeroActivationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroActivationResponseDto;
import za.co.mawa.bes.dto.v2.integration.XeroConnectionDto;
import za.co.mawa.bes.dto.v2.integration.XeroInvoiceIntegrationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroSelectTenantRequestDto;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.xero.XeroAuthService;
import za.co.mawa.bes.xero.XeroMasterDataQueueService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class XeroActivationService {

    private static final Logger log = LoggerFactory.getLogger(XeroActivationService.class);
    private static final String XERO_GROUP = "XERO";
    private static final String INVOICE_ENABLED = "INVOICE-INTEGRATION-ENABLED";
    private static final String INVOICE_REQUESTED = "INVOICE-INTEGRATION-REQUESTED";
    private static final String INTEGRATION_ENABLED = "INTEGRATION";
    private static final String INTEGRATION_STATUS = "INTEGRATION-STATUS";

    private final GcpTenantSecretService gcpTenantSecretService;
    private final TenantSecretNameService tenantSecretNameService;
    private final SettingService settingService;
    private final Environment environment;
    private final XeroAuthService xeroAuthService;
    private final XeroMasterDataQueueService xeroMasterDataQueueService;

    public XeroActivationService(GcpTenantSecretService gcpTenantSecretService,
                                 TenantSecretNameService tenantSecretNameService,
                                 SettingService settingService,
                                 Environment environment,
                                 XeroAuthService xeroAuthService,
                                 XeroMasterDataQueueService xeroMasterDataQueueService) {
        this.gcpTenantSecretService = gcpTenantSecretService;
        this.tenantSecretNameService = tenantSecretNameService;
        this.settingService = settingService;
        this.environment = environment;
        this.xeroAuthService = xeroAuthService;
        this.xeroMasterDataQueueService = xeroMasterDataQueueService;
    }

    public XeroActivationResponseDto activate(XeroActivationRequestDto request) {
        String tenant = TenantContext.getCurrentTenant();
        if (!StringUtils.hasText(tenant)) {
            throw new IllegalStateException("Tenant is required to activate Xero integration");
        }
        if (request == null || !StringUtils.hasText(request.getClientId())) {
            throw new IllegalArgumentException("Xero Client ID is required");
        }
        if (!StringUtils.hasText(request.getClientSecret())) {
            throw new IllegalArgumentException("Xero Client Secret is required");
        }

        String tenantHost = tenantSecretNameService.resolveCurrentTenantHost();
        String clientIdSecret = tenantSecretNameService.currentTenantSecretName("xero", "client-id");
        String clientSecretSecret = tenantSecretNameService.currentTenantSecretName("xero", "secret-key");
        String refreshTokenSecret = tenantSecretNameService.currentTenantSecretName("xero", "refresh-token");
        String tenantIdSecret = tenantSecretNameService.currentTenantSecretName("xero", "tenant-id");
        String accessTokenSecret = tenantSecretNameService.currentTenantSecretName("xero", "access-token");

        gcpTenantSecretService.createOrAddSecretVersion(clientIdSecret, request.getClientId().trim());
        gcpTenantSecretService.createOrAddSecretVersion(clientSecretSecret, request.getClientSecret().trim());
        gcpTenantSecretService.createSecretIfMissing(refreshTokenSecret);
        gcpTenantSecretService.createSecretIfMissing(tenantIdSecret);
        gcpTenantSecretService.createSecretIfMissing(accessTokenSecret);

        String redirectUrl = buildRedirectUrl(request.getRedirectUrl());
        boolean enableInvoices = request.getInvoiceIntegrationEnabled() == null || request.getInvoiceIntegrationEnabled();

        settingService.upsertSetting("CLIENT-ID-SECRET", XERO_GROUP, clientIdSecret);
        settingService.upsertSetting("SECRET-KEY-SECRET", XERO_GROUP, clientSecretSecret);
        settingService.upsertSetting("REFRESH-TOKEN-SECRET", XERO_GROUP, refreshTokenSecret);
        settingService.upsertSetting("ACCESS-TOKEN-SECRET", XERO_GROUP, accessTokenSecret);
        settingService.upsertSetting("TENANT-ID-SECRET", XERO_GROUP, tenantIdSecret);
        settingService.upsertSetting("REDIRECT-URL", XERO_GROUP, redirectUrl);
        if (StringUtils.hasText(request.getPaymentAccountCode())) {
            settingService.upsertSetting("PAYMENT-ACCOUNT-CODE", XERO_GROUP,
                    request.getPaymentAccountCode().trim());
        }
        // Remember the user's preference, but do not allow background synchronisation
        // until the OAuth callback has completed and an organisation is selected.
        settingService.upsertSetting(INVOICE_REQUESTED, XERO_GROUP, String.valueOf(enableInvoices));
        setRuntimeState(false, "PENDING_AUTHORISATION");

        String authenticationUrl = buildAuthenticationUrl(request.getClientId().trim(), redirectUrl, tenantHost);

        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(false)
                .invoiceIntegrationRequested(enableInvoices)
                .integrationStatus("PENDING_AUTHORISATION")
                .authenticationUrl(authenticationUrl)
                .clientIdSecret(clientIdSecret)
                .clientSecretSecret(clientSecretSecret)
                .refreshTokenSecret(refreshTokenSecret)
                .tenantIdSecret(tenantIdSecret)
                .accessTokenSecret(accessTokenSecret)
                .redirectUrl(redirectUrl)
                .paymentAccountCode(settingService.getSetting("PAYMENT-ACCOUNT-CODE", XERO_GROUP))
                .organisationSelectionRequired(false)
                .message("Xero secrets were saved to Google Secret Manager. Open authenticationUrl to authorise the Xero organisation.")
                .build();
    }

    public XeroActivationResponseDto secretNames() {
        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(isEnabled(INVOICE_ENABLED))
                .invoiceIntegrationRequested(invoiceIntegrationRequested())
                .integrationStatus(currentStatus())
                .organisationSelectionRequired(false)
                .clientIdSecret(tenantSecretNameService.currentTenantSecretName("xero", "client-id"))
                .clientSecretSecret(tenantSecretNameService.currentTenantSecretName("xero", "secret-key"))
                .refreshTokenSecret(tenantSecretNameService.currentTenantSecretName("xero", "refresh-token"))
                .tenantIdSecret(tenantSecretNameService.currentTenantSecretName("xero", "tenant-id"))
                .accessTokenSecret(tenantSecretNameService.currentTenantSecretName("xero", "access-token"))
                .redirectUrl(settingService.getSetting("REDIRECT-URL", XERO_GROUP))
                .paymentAccountCode(settingService.getSetting("PAYMENT-ACCOUNT-CODE", XERO_GROUP))
                .build();
    }

    public java.util.List<XeroConnectionDto> connections() {
        String status = settingService.getSetting(INTEGRATION_STATUS, XERO_GROUP);
        if (isPreAuthorisationStatus(status)) {
            return Collections.emptyList();
        }
        try {
            return xeroAuthService.getConnectionsForCurrentTenant();
        } catch (Exception e) {
            if (isInvalidGrant(e)) {
                markReauthorisationRequired();
                log.warn("Xero authorisation requires reconnection for tenant {}: {}",
                        TenantContext.getCurrentTenant(), rootMessage(e));
                return Collections.emptyList();
            }
            if (isMissingConfiguration(e)) {
                setRuntimeState(false, "NOT_AUTHORISED");
                log.info("Xero is not yet authorised for tenant {}: {}",
                        TenantContext.getCurrentTenant(), rootMessage(e));
                return Collections.emptyList();
            }
            throw new IllegalStateException("Unable to retrieve Xero organisations. Please retry or reconnect Xero.", e);
        }
    }

    public XeroActivationResponseDto selectTenant(XeroSelectTenantRequestDto request) {
        try {
            String tenant = TenantContext.getCurrentTenant();
            XeroConnectionDto selected = xeroAuthService.selectXeroTenant(tenant, request == null ? null : request.getTenantId());
            boolean enabled = enableAfterAuthorisation();
            return XeroActivationResponseDto.builder()
                    .invoiceIntegrationEnabled(enabled)
                    .invoiceIntegrationRequested(invoiceIntegrationRequested())
                    .integrationStatus("AUTHORISED")
                    .organisationSelectionRequired(false)
                    .selectedTenantId(selected.getTenantId())
                    .selectedTenantName(selected.getTenantName())
                    .message(enabled
                            ? "Xero organisation selected. Existing products, customers and invoices were queued for synchronisation."
                            : "Xero organisation selected. Synchronisation remains disabled as requested.")
                    .build();
        } catch (Exception e) {
            if (isInvalidGrant(e)) {
                setRuntimeState(false, "REAUTHORISATION_REQUIRED");
                throw new IllegalStateException("Xero authorisation has expired or the stored refresh token is invalid. Click Activate / Reconnect Xero again before selecting the organisation.", e);
            }
            throw new IllegalStateException("Unable to save selected Xero organisation", e);
        }
    }

    public XeroActivationResponseDto deactivate() {
        settingService.upsertSetting(INVOICE_REQUESTED, XERO_GROUP, "false");
        setRuntimeState(false, "DISABLED");
        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(false)
                .invoiceIntegrationRequested(false)
                .integrationStatus("DISABLED")
                .organisationSelectionRequired(false)
                .message("Xero customer, product and invoice synchronisation is disabled for this tenant. Secret references were retained for future reactivation.")
                .build();
    }

    public XeroActivationResponseDto updateInvoiceIntegration(XeroInvoiceIntegrationRequestDto request) {
        if (request == null || request.getEnabled() == null) {
            throw new IllegalArgumentException("Invoice integration enabled value is required");
        }

        boolean requested = request.getEnabled();
        String status = currentStatus();
        boolean authorised = "AUTHORISED".equalsIgnoreCase(status);
        settingService.upsertSetting(INVOICE_REQUESTED, XERO_GROUP, String.valueOf(requested));

        boolean runtimeEnabled = requested && authorised;
        setRuntimeState(runtimeEnabled, status);
        if (runtimeEnabled) {
            xeroMasterDataQueueService.queueAllExisting();
        }

        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(runtimeEnabled)
                .invoiceIntegrationRequested(requested)
                .integrationStatus(status)
                .organisationSelectionRequired("PENDING_ORGANISATION_SELECTION".equalsIgnoreCase(status))
                .message(runtimeEnabled
                        ? "Xero invoice integration enabled. Existing products, customers and invoices were queued for synchronisation."
                        : requested
                                ? "Invoice integration will be enabled after Xero authorisation is completed."
                                : "Xero invoice integration disabled.")
                .build();
    }

    private boolean isInvalidGrant(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("invalid_grant")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isMissingConfiguration(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalised = message.toLowerCase();
                if (normalised.contains("missing required xero configuration")
                        || normalised.contains("secret does not contain any versions")
                        || normalised.contains("secret version") && normalised.contains("not found")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isPreAuthorisationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return switch (status.trim().toUpperCase()) {
            case "PENDING_AUTHORISATION", "PENDING_ORGANISATION_SELECTION",
                    "NOT_AUTHORISED", "REAUTHORISATION_REQUIRED", "DISABLED" -> true;
            default -> false;
        };
    }

    private void markReauthorisationRequired() {
        setRuntimeState(false, "REAUTHORISATION_REQUIRED");
    }

    /** Finalises activation after Xero has exchanged the code and resolved the organisation. */
    public void completeAuthorisation(XeroAuthService.XeroOAuthResult result) {
        if (result == null || result.isOrganisationSelectionRequired()) {
            setRuntimeState(false, "PENDING_ORGANISATION_SELECTION");
            return;
        }
        enableAfterAuthorisation();
    }

    private boolean enableAfterAuthorisation() {
        boolean enabled = invoiceIntegrationRequested();
        setRuntimeState(enabled, "AUTHORISED");
        if (enabled) {
            xeroMasterDataQueueService.queueAllExisting();
        }
        return enabled;
    }

    private boolean invoiceIntegrationRequested() {
        String requestedValue = settingService.getSetting(INVOICE_REQUESTED, XERO_GROUP);
        return !StringUtils.hasText(requestedValue) || Boolean.parseBoolean(requestedValue);
    }

    private void setRuntimeState(boolean enabled, String status) {
        settingService.upsertSetting(INVOICE_ENABLED, XERO_GROUP, String.valueOf(enabled));
        settingService.upsertSetting(INTEGRATION_ENABLED, XERO_GROUP, String.valueOf(enabled));
        settingService.upsertSetting(INTEGRATION_STATUS, XERO_GROUP, status);
    }

    private boolean isEnabled(String attribute) {
        return Boolean.parseBoolean(settingService.getSetting(attribute, XERO_GROUP));
    }

    private String currentStatus() {
        String status = settingService.getSetting(INTEGRATION_STATUS, XERO_GROUP);
        if (StringUtils.hasText(status)) {
            return status;
        }
        return isEnabled(INVOICE_ENABLED) ? "AUTHORISED" : "NOT_AUTHORISED";
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        Throwable last = error;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        return last == null || last.getMessage() == null ? "Unknown Xero error" : last.getMessage();
    }

    private String buildAuthenticationUrl(String clientId, String redirectUrl, String tenant) {
        return XeroAuthService.getAUTH_URL() + "?response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)
                + "&scope=" + encodeOAuthScope(XeroAuthService.getSCOPES())
                + "&state=" + URLEncoder.encode(tenant, StandardCharsets.UTF_8);
    }

    private String encodeOAuthScope(String scopes) {
        // Scope is a space-delimited OAuth value. Emit RFC 3986 spaces explicitly;
        // URLEncoder uses '+', which Xero can interpret as part of the scope name.
        return URLEncoder.encode(scopes, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildRedirectUrl(String rawRedirectUrl) {
        String baseUrl = StringUtils.hasText(rawRedirectUrl)
                ? rawRedirectUrl.trim()
                : firstNonBlank(environment.getProperty("mawa.public-api-url"), environment.getProperty("MAWA_PUBLIC_API_URL"));
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("Redirect URL is required. Use the public backend base URL, for example https://dev.api.app.mawa.co.za");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        return baseUrl.endsWith("/xero/callback") ? baseUrl : baseUrl + "/xero/callback";
    }
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
