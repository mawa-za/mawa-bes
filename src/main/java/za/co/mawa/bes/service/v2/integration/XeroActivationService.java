package za.co.mawa.bes.service.v2.integration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.dto.v2.integration.XeroActivationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroActivationResponseDto;
import za.co.mawa.bes.dto.v2.integration.XeroConnectionDto;
import za.co.mawa.bes.dto.v2.integration.XeroSelectTenantRequestDto;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.xero.XeroAuthService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class XeroActivationService {

    private static final String XERO_GROUP = "XERO";

    private final GcpTenantSecretService gcpTenantSecretService;
    private final TenantSecretNameService tenantSecretNameService;
    private final SettingService settingService;
    private final Environment environment;
    private final XeroAuthService xeroAuthService;

    public XeroActivationService(GcpTenantSecretService gcpTenantSecretService,
                                 TenantSecretNameService tenantSecretNameService,
                                 SettingService settingService,
                                 Environment environment,
                                 XeroAuthService xeroAuthService) {
        this.gcpTenantSecretService = gcpTenantSecretService;
        this.tenantSecretNameService = tenantSecretNameService;
        this.settingService = settingService;
        this.environment = environment;
        this.xeroAuthService = xeroAuthService;
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
        settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", XERO_GROUP, String.valueOf(enableInvoices));

        String authenticationUrl = buildAuthenticationUrl(request.getClientId().trim(), redirectUrl, tenantHost);

        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(enableInvoices)
                .authenticationUrl(authenticationUrl)
                .clientIdSecret(clientIdSecret)
                .clientSecretSecret(clientSecretSecret)
                .refreshTokenSecret(refreshTokenSecret)
                .tenantIdSecret(tenantIdSecret)
                .accessTokenSecret(accessTokenSecret)
                .redirectUrl(redirectUrl)
                .organisationSelectionRequired(false)
                .message("Xero secrets were saved to Google Secret Manager. Open authenticationUrl to authorise the Xero organisation.")
                .build();
    }

    public XeroActivationResponseDto secretNames() {
        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(Boolean.parseBoolean(settingService.getSetting("INVOICE-INTEGRATION-ENABLED", XERO_GROUP)))
                .organisationSelectionRequired(false)
                .clientIdSecret(tenantSecretNameService.currentTenantSecretName("xero", "client-id"))
                .clientSecretSecret(tenantSecretNameService.currentTenantSecretName("xero", "secret-key"))
                .refreshTokenSecret(tenantSecretNameService.currentTenantSecretName("xero", "refresh-token"))
                .tenantIdSecret(tenantSecretNameService.currentTenantSecretName("xero", "tenant-id"))
                .accessTokenSecret(tenantSecretNameService.currentTenantSecretName("xero", "access-token"))
                .redirectUrl(settingService.getSetting("REDIRECT-URL", XERO_GROUP))
                .build();
    }

    public java.util.List<XeroConnectionDto> connections() {
        try {
            return xeroAuthService.getConnectionsForCurrentTenant();
        } catch (Exception e) {
            if (isInvalidGrant(e)) {
                settingService.upsertSetting("INTEGRATION-STATUS", XERO_GROUP, "REAUTHORISATION_REQUIRED");
                settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", XERO_GROUP, "false");
                throw new IllegalStateException("Xero authorisation has expired or the stored refresh token is invalid. Click Activate / Reconnect Xero again to authorise the organisation.", e);
            }
            throw new IllegalStateException("Unable to retrieve Xero organisations. Complete Xero authorisation first.", e);
        }
    }

    public XeroActivationResponseDto selectTenant(XeroSelectTenantRequestDto request) {
        try {
            String tenant = TenantContext.getCurrentTenant();
            XeroConnectionDto selected = xeroAuthService.selectXeroTenant(tenant, request == null ? null : request.getTenantId());
            return XeroActivationResponseDto.builder()
                    .invoiceIntegrationEnabled(true)
                    .organisationSelectionRequired(false)
                    .selectedTenantId(selected.getTenantId())
                    .selectedTenantName(selected.getTenantName())
                    .message("Xero organisation selected and invoice integration enabled.")
                    .build();
        } catch (Exception e) {
            if (isInvalidGrant(e)) {
                settingService.upsertSetting("INTEGRATION-STATUS", XERO_GROUP, "REAUTHORISATION_REQUIRED");
                settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", XERO_GROUP, "false");
                throw new IllegalStateException("Xero authorisation has expired or the stored refresh token is invalid. Click Activate / Reconnect Xero again before selecting the organisation.", e);
            }
            throw new IllegalStateException("Unable to save selected Xero organisation", e);
        }
    }

    public XeroActivationResponseDto deactivate() {
        settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", XERO_GROUP, "false");
        return XeroActivationResponseDto.builder()
                .invoiceIntegrationEnabled(false)
                .organisationSelectionRequired(false)
                .message("Xero invoice integration disabled for this tenant. Secret references were retained for future reactivation.")
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

    private String buildAuthenticationUrl(String clientId, String redirectUrl, String tenant) {
        return XeroAuthService.getAUTH_URL() + "?response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(XeroAuthService.getSCOPES(), StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(tenant, StandardCharsets.UTF_8);
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
