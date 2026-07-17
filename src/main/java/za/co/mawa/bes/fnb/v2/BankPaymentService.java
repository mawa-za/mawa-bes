package za.co.mawa.bes.fnb.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.dto.OAuthTokenResponse;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.fnb.FnbApiCallLogger;
import za.co.mawa.bes.fnb.dto.*;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.TransactionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service(value = "bankPaymentServiceV2")
@RequiredArgsConstructor
public class BankPaymentService {

    private final SettingService settingService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final GcpTenantSecretService gcpTenantSecretService;
    private final FnbApiCallLogger fnbApiCallLogger;
    private final JdbcTemplate jdbcTemplate;

    private static final String FNB_API_SETTING_GROUP = "FNB-API";
    private static final String TENANT_SETTING_GROUP = "TENANT";
    private static final String EFT_BANK_ACCOUNT_SETTING_GROUP = "EFT-BANK-ACCOUNT";

    private static final String PAYMENT_METHOD_TRANSFER = "TRF";
    private static final String SERVICE_LEVEL_CODE = "SDVA";
    private static final String REMITTANCE_LOCATION_METHOD = "EMAL";

    private static final int END_TO_END_MAX_LENGTH = 30;
    private static final int REMITTANCE_MAX_LENGTH = 35;

    private String getBaseURL() {
        String baseUrl = gcpTenantSecretService.resolveSetting("BASE-URL", FNB_API_SETTING_GROUP);

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new RuntimeException("FNB API base URL setting is missing");
        }

        return baseUrl;
    }

    public String getToken() {
        HttpURLConnection connection = null;
        String requestId = fnbApiCallLogger.newRequestId();
        String endpoint = "FNB /oauth2/token/v2";
        String responseBody = null;
        Integer responseCode = null;
        Throwable failure = null;
        long startedAt = System.nanoTime();

        try {
            endpoint = getBaseURL() + "/oauth2/token/v2";
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String clientId = gcpTenantSecretService.resolveSetting("CLIENT-ID", FNB_API_SETTING_GROUP);
            String clientSecret = gcpTenantSecretService.resolveSetting("CLIENT-SECRET", FNB_API_SETTING_GROUP);

            if (isBlank(clientId) || isBlank(clientSecret)) {
                throw new RuntimeException("FNB client credentials are missing");
            }

            String basicAuth = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            connection.setRequestProperty("Authorization", "Basic " + basicAuth);

            String requestBody = "grant_type=client_credentials&scope=i_can";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            responseCode = connection.getResponseCode();
            responseBody = readResponseBody(connection);

            if (responseCode >= 200 && responseCode < 300) {
                OAuthTokenResponse tokenResponse =
                        objectMapper.readValue(responseBody, OAuthTokenResponse.class);

                if (tokenResponse == null || isBlank(tokenResponse.getAccessToken())) {
                    throw new RuntimeException("FNB token response did not contain an access token");
                }

                return tokenResponse.getAccessToken();
            }

            throw new RuntimeException(
                    "Failed to retrieve FNB token. Response code: " + responseCode + ". Response: " + responseBody
            );

        } catch (Exception e) {
            failure = e;
            throw new RuntimeException("Failed to retrieve FNB access token", e);
        } finally {
            fnbApiCallLogger.logCall(
                    requestId,
                    "POST",
                    endpoint,
                    fnbApiCallLogger.tokenRequestSummary(),
                    responseCode,
                    responseBody,
                    elapsedMillis(startedAt),
                    failure
            );
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String sendPaymentRequest(String payload) throws IOException {
        HttpURLConnection connection = null;
        String requestId = fnbApiCallLogger.newRequestId();
        String endpoint = "FNB /paymentExecution/initiate/v1";
        String responseBody = null;
        Integer responseCode = null;
        Throwable failure = null;
        long startedAt = System.nanoTime();

        try {
            endpoint = getBaseURL() + "/paymentExecution/initiate/v1";
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + getToken());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            responseCode = connection.getResponseCode();
            responseBody = readResponseBody(connection);

            if (responseCode >= 200 && responseCode < 300) {
                BankPaymentResponse bankPaymentResponse =
                        objectMapper.readValue(responseBody, BankPaymentResponse.class);

                return bankPaymentResponse.getInstructionId();
            }

            throw new IOException(
                    "FNB payment request failed. Response code: " + responseCode + ". Response: " + responseBody
            );

        } catch (SocketTimeoutException e) {
            failure = e;
            throw new IOException("FNB payment request timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            failure = e;
            throw new IOException("Failed to send FNB payment request: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            fnbApiCallLogger.logCall(
                    requestId,
                    "POST",
                    endpoint,
                    payload,
                    responseCode,
                    responseBody,
                    elapsedMillis(startedAt),
                    failure
            );
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public BankPaymentResponse getPaymentReport(String instructionId) throws IOException {
        HttpURLConnection connection = null;
        String requestId = fnbApiCallLogger.newRequestId();
        String endpoint = "FNB /paymentExecution/retrieveReport/v1/" + instructionId;
        String responseBody = null;
        Integer responseCode = null;
        Throwable failure = null;
        long startedAt = System.nanoTime();

        try {
            if (isBlank(instructionId)) {
                throw new IOException("Instruction ID is required to retrieve FNB payment report");
            }

            endpoint = getBaseURL() + "/paymentExecution/retrieveReport/v1/" + instructionId;
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + getToken());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            responseCode = connection.getResponseCode();
            responseBody = readResponseBody(connection);

            if (responseCode >= 200 && responseCode < 300) {
                return objectMapper.readValue(responseBody, BankPaymentResponse.class);
            }

            throw new IOException(
                    "FNB report retrieval failed. Response code: " + responseCode + ". Response: " + responseBody
            );

        } catch (SocketTimeoutException e) {
            failure = e;
            throw new IOException("FNB report retrieval timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            failure = e;
            throw new IOException("Failed to retrieve FNB payment report: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            fnbApiCallLogger.logCall(
                    requestId,
                    "GET",
                    endpoint,
                    null,
                    responseCode,
                    responseBody,
                    elapsedMillis(startedAt),
                    failure
            );
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public BankPaymentRequest generateRequest(PaymentRequestEntity paymentRequest) {
        validatePaymentRequest(paymentRequest);

        try {
            BankPaymentRequest bankPaymentRequest = new BankPaymentRequest();

            bankPaymentRequest.setGroupHeader(groupHeader(paymentRequest));

            List<PaymentInformation> paymentInformationList = new ArrayList<>();
            paymentInformationList.add(paymentInformation(paymentRequest));

            bankPaymentRequest.setPaymentInformation(paymentInformationList);

            return bankPaymentRequest;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate FNB bank payment request", e);
        }
    }

    private GroupHeader groupHeader(PaymentRequestEntity paymentRequest) {
        GroupHeader groupHeader = new GroupHeader();

        TransactionDto paymentBatch = createPaymentBatch();

        groupHeader.setMessageId(paymentBatch.getId());
        groupHeader.setCreationDateTime(resolveCreationDateTime());
        groupHeader.setTotalNumberOfTransactions(1);
        groupHeader.setTotalControlSum(toDouble(paymentRequest.getAmount()));
        groupHeader.setInitiatingPartyName(requiredSetting("COMPANY-NAME", TENANT_SETTING_GROUP));

        return groupHeader;
    }

    private PaymentInformation paymentInformation(PaymentRequestEntity paymentRequest) {
        PaymentInformation paymentInformation = new PaymentInformation();

        paymentInformation.setPaymentInformationId(paymentRequest.getRequestNo());
        paymentInformation.setPaymentInformationMethod(PAYMENT_METHOD_TRANSFER);
        paymentInformation.setBatchBooking(false);
        paymentInformation.setNumberOfTransactions(1);
        paymentInformation.setControlSum(toDouble(paymentRequest.getAmount()));
        paymentInformation.setPaymentTypeInformationServiceLevelCode(SERVICE_LEVEL_CODE);
        paymentInformation.setRequestedExecutionDate(Conversion.dateToString(resolveExecutionDate(paymentRequest)));

        paymentInformation.setDebtor(buildDebtor(paymentRequest));
        paymentInformation.setDebtorAccount(buildDebtorAccount(paymentRequest));
        paymentInformation.setDebtorAgent(buildDebtorAgent(paymentRequest));

        CreditTransferTransactionInformation transactionInformation =
                buildCreditTransferTransactionInformation(paymentRequest);

        List<CreditTransferTransactionInformation> transactionInformationList = new ArrayList<>();
        transactionInformationList.add(transactionInformation);

        paymentInformation.setCreditTransferTransactionInformation(transactionInformationList);

        return paymentInformation;
    }

    private CreditTransferTransactionInformation buildCreditTransferTransactionInformation(
            PaymentRequestEntity paymentRequest
    ) {
        CreditTransferTransactionInformation transactionInformation =
                new CreditTransferTransactionInformation();

        Amount amount = new Amount();
        amount.setCurrency(resolveCurrency(paymentRequest));
        amount.setValue(toDouble(paymentRequest.getAmount()));
        transactionInformation.setAmount(amount);

        Creditor creditor = new Creditor();
        creditor.setName(resolveCreditorName(paymentRequest));
        creditor.setBicOrBEI(resolveCreditorBranchCode(paymentRequest));
        transactionInformation.setCreditor(creditor);

        CreditorAccount creditorAccount = new CreditorAccount();
        creditorAccount.setAccountNumber(resolveCreditorAccountNumber(paymentRequest));
        creditorAccount.setAccountType(toFnbAccountType(paymentRequest.getAccountType()));
        transactionInformation.setCreditorAccount(creditorAccount);

        CreditorAgent creditorAgent = new CreditorAgent();
        creditorAgent.setBranchId(resolveCreditorBranchCode(paymentRequest));
        transactionInformation.setCreditorAgent(creditorAgent);

        String endToEndId = paymentRequest.getRequestNo() + "-" + resolvePaymentReason(paymentRequest);
        transactionInformation.setEndToEndId(limit(endToEndId, END_TO_END_MAX_LENGTH));

        transactionInformation.setRemittanceInformationUnstructured(
                limit(resolvePaymentReference(paymentRequest), REMITTANCE_MAX_LENGTH)
        );

        transactionInformation.setRemittanceLocationMethod(REMITTANCE_LOCATION_METHOD);
        transactionInformation.setRemittanceLocationElectronicAddress(
                requiredSetting("POP-RECIPIENT", FNB_API_SETTING_GROUP)
        );

        return transactionInformation;
    }

    private java.util.Map<String,Object> debtorAccount(PaymentRequestEntity paymentRequest) {
        if (isBlank(paymentRequest.getDebtorAccountId())) {
            throw new RuntimeException("Configured debtor account is missing for payment request: " + paymentRequest.getRequestNo());
        }
        var rows = jdbcTemplate.queryForList("SELECT * FROM payment_bank_account WHERE id=? AND account_role='DEBTOR' AND active=1", paymentRequest.getDebtorAccountId());
        if (rows.isEmpty()) throw new RuntimeException("Configured debtor account is inactive or missing: " + paymentRequest.getDebtorAccountId());
        var row = rows.get(0);
        if (!"FNB".equalsIgnoreCase(java.util.Objects.toString(row.get("bank_integration"), "")))
            throw new RuntimeException("Only FNB debtor accounts can currently be automated");
        return row;
    }

    private Debtor buildDebtor(PaymentRequestEntity paymentRequest) {
        var account = debtorAccount(paymentRequest);
        Debtor debtor = new Debtor();
        debtor.setName(java.util.Objects.toString(account.get("account_holder"), null));
        debtor.setBicOrBEI(java.util.Objects.toString(account.get("branch_code"), null));
        return debtor;
    }

    private DebtorAccount buildDebtorAccount(PaymentRequestEntity paymentRequest) {
        var account = debtorAccount(paymentRequest);
        DebtorAccount debtorAccount = new DebtorAccount();
        debtorAccount.setAccountNumber(java.util.Objects.toString(account.get("account_number"), null));
        debtorAccount.setAccountType(toFnbAccountType(java.util.Objects.toString(account.get("account_type"), null)));
        return debtorAccount;
    }

    private DebtorAgent buildDebtorAgent(PaymentRequestEntity paymentRequest) {
        var account = debtorAccount(paymentRequest);
        DebtorAgent debtorAgent = new DebtorAgent();
        debtorAgent.setBranchId(java.util.Objects.toString(account.get("branch_code"), null));
        return debtorAgent;
    }

    private void validatePaymentRequest(PaymentRequestEntity paymentRequest) {
        if (paymentRequest == null) {
            throw new RuntimeException("Payment request is required");
        }

        if (isBlank(paymentRequest.getId())) {
            throw new RuntimeException("Payment request ID is required");
        }

        if (isBlank(paymentRequest.getRequestNo())) {
            throw new RuntimeException("Payment request number is required");
        }

        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment request amount must be greater than zero");
        }

        if (paymentRequest.getPaymentMethod() != PaymentMethod.EFT) {
            throw new RuntimeException(
                    "Only EFT payment requests can be sent to FNB. Payment request: " + paymentRequest.getRequestNo()
            );
        }

        if (isBlank(paymentRequest.getAccountNumber())) {
            throw new RuntimeException("Creditor account number is required");
        }

        if (isBlank(paymentRequest.getBranchCode())) {
            throw new RuntimeException("Creditor branch code is required");
        }

        if (isBlank(paymentRequest.getAccountType())) {
            throw new RuntimeException("Creditor account type is required");
        }

        if (isBlank(resolveCreditorName(paymentRequest))) {
            throw new RuntimeException("Creditor name is required");
        }
    }

    private Date resolveExecutionDate(PaymentRequestEntity paymentRequest) {
        if (paymentRequest.getRequestedPaymentDate() == null) {
            return new Date();
        }

        Date requestedDate = Date.from(
                paymentRequest.getRequestedPaymentDate()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        Date today = new Date();

        if (requestedDate.after(today)) {
            return requestedDate;
        }

        return today;
    }

    private String resolveCreationDateTime() {
        try {
            String dateSetting = gcpTenantSecretService.resolveSetting("PAYMENT-CREATION-DATE", FNB_API_SETTING_GROUP);

            if (!isBlank(dateSetting)) {
                return dateSetting;
            }

            return Conversion.dateToString(new Date());
        } catch (Exception e) {
            settingService.createSetting("PAYMENT-CREATION-DATE", FNB_API_SETTING_GROUP, "");
            return Conversion.dateToString(new Date());
        }
    }

    private String resolveCurrency(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getCurrency())) {
            return paymentRequest.getCurrency();
        }

        String tenantCurrency = gcpTenantSecretService.resolveSetting("CURRENCY", TENANT_SETTING_GROUP);

        if (!isBlank(tenantCurrency)) {
            return tenantCurrency;
        }

        return "ZAR";
    }

    private String resolveCreditorName(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getAccountHolder())) {
            return paymentRequest.getAccountHolder();
        }

        if (!isBlank(paymentRequest.getPayeeName())) {
            return paymentRequest.getPayeeName();
        }

        return null;
    }

    private String resolveCreditorBranchCode(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getBranchCode())) {
            return paymentRequest.getBranchCode();
        }

        throw new RuntimeException("Creditor branch code is required for payment request: " + paymentRequest.getId());
    }

    private String resolveCreditorAccountNumber(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getAccountNumber())) {
            return paymentRequest.getAccountNumber();
        }

        throw new RuntimeException("Creditor account number is required for payment request: " + paymentRequest.getId());
    }

    private String resolvePaymentReference(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getExternalReference())) {
            return paymentRequest.getExternalReference();
        }

        if (!isBlank(paymentRequest.getInvoiceNo())) {
            return paymentRequest.getInvoiceNo();
        }

        if (!isBlank(paymentRequest.getPaymentReason())) {
            return paymentRequest.getPaymentReason();
        }

        return paymentRequest.getRequestNo();
    }

    private String resolvePaymentReason(PaymentRequestEntity paymentRequest) {
        if (!isBlank(paymentRequest.getPaymentReason())) {
            return paymentRequest.getPaymentReason();
        }

        if (paymentRequest.getRequestType() != null) {
            return paymentRequest.getRequestType().name();
        }

        return "PAYMENT";
    }

    private String toFnbAccountType(String accountType) {
        if (isBlank(accountType)) {
            throw new RuntimeException("Account type is required");
        }

        String value = accountType.trim().toUpperCase();

        if (
                value.equals("CHEQUE") ||
                        value.equals("CURRENT") ||
                        value.equals("CACC")
        ) {
            return "CACC";
        }

        if (
                value.equals("SAVINGS") ||
                        value.equals("SAVING") ||
                        value.equals("SVGS")
        ) {
            return "SVGS";
        }

        if (
                value.equals("TRANSMISSION") ||
                        value.equals("TRANSACTION") ||
                        value.equals("TRAN")
        ) {
            return "CACC";
        }

        throw new RuntimeException("Unsupported FNB account type: " + accountType);
    }

    private double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.00;
        }

        return value.doubleValue();
    }

    private String requiredSetting(String key, String group) {
        String value = gcpTenantSecretService.resolveSetting(key, group);

        if (isBlank(value)) {
            throw new RuntimeException("Missing required setting: " + group + " / " + key + ". Store a Google Secret Manager reference in " + key + "-SECRET or configure the tenant setting.");
        }

        return value;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream stream;

        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
            stream = connection.getInputStream();
        } else {
            stream = connection.getErrorStream();
        }

        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }

    public TransactionDto createPaymentBatch() {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PAYMENT_BATCH);
        return transactionService.create(transactionCreateDto);
    }
}