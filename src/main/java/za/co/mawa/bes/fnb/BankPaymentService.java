package za.co.mawa.bes.fnb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.OAuthTokenResponse;
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkInboundDto;
import za.co.mawa.bes.fnb.dto.*;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.TransactionLinkType;
import za.co.mawa.bes.utils.TransactionType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class BankPaymentService {
    @Autowired
    SettingService settingService;
    @Autowired
    GcpTenantSecretService gcpTenantSecretService;
    @Autowired
    PartnerIdentityService partnerIdentityService;
    @Autowired
    FnbApiCallLogger fnbApiCallLogger;

    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    TransactionService transactionService;

    private String getBaseURL() {
        return gcpTenantSecretService.resolveSetting("BASE-URL", "FNB-API");
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

            String clientId = gcpTenantSecretService.resolveSetting("CLIENT-ID", "FNB-API");
            String clientSecret = gcpTenantSecretService.resolveSetting("CLIENT-SECRET", "FNB-API");
            String basicAuth = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
            );
            connection.setRequestProperty("Authorization", "Basic " + basicAuth);

            String data = "grant_type=client_credentials&scope=i_can";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            responseCode = connection.getResponseCode();
            responseBody = readResponseBody(connection);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(
                        "FNB token request failed with code " + responseCode + ". Response: " + responseBody
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            OAuthTokenResponse tokenResponse = mapper.readValue(responseBody, OAuthTokenResponse.class);
            return tokenResponse.getAccessToken();
        } catch (Exception e) {
            failure = e;
            return "";
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
                ObjectMapper mapper = new ObjectMapper();
                BankPaymentResponse bankPaymentResponse = mapper.readValue(
                        responseBody,
                        BankPaymentResponse.class
                );
                return bankPaymentResponse.getInstructionId();
            }

            throw new IOException(String.format(
                    "Request failed with code: %d. Response: %s",
                    responseCode,
                    responseBody
            ));
        } catch (SocketTimeoutException e) {
            failure = e;
            throw new IOException("Request timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            failure = e;
            throw new IOException("Failed to send payment request: " + e.getMessage(), e);
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
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(responseBody, BankPaymentResponse.class);
            }

            throw new IOException(String.format(
                    "Request failed with code: %d. Response: %s",
                    responseCode,
                    responseBody
            ));
        } catch (SocketTimeoutException e) {
            failure = e;
            throw new IOException("Request timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            failure = e;
            throw new IOException("Failed to retrieve report: " + e.getMessage(), e);
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

    public BankPaymentRequest generateRequest(PaymentRequestDto paymentRequestDto) {
        BankPaymentRequest bankPaymentRequest = new BankPaymentRequest();
        try {
            bankPaymentRequest.setGroupHeader(groupHeader(paymentRequestDto));
            List<PaymentInformation> paymentInformationList = new ArrayList<>();
            paymentInformationList.add(paymentInformation(paymentRequestDto));
            bankPaymentRequest.setPaymentInformation(paymentInformationList);
            return bankPaymentRequest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GroupHeader groupHeader(PaymentRequestDto paymentRequestDto) {
        GroupHeader grpHdr = new GroupHeader();
        try {
            grpHdr.setMessageId(createPaymentBatch().getId());
            Instant instant = new Date().toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);
            String isoDate = zdt.format(DateTimeFormatter.ISO_DATE_TIME);

            try {
                String dateSetting = gcpTenantSecretService.resolveSetting("PAYMENT-CREATION-DATE", "FNB-API");
                if (!dateSetting.isEmpty()) {
                    String creationDate = dateSetting;
                    grpHdr.setCreationDateTime(creationDate);
                } else {
                    String creationDate = Conversion.dateToString(new Date());
                    grpHdr.setCreationDateTime(creationDate);
                }
            } catch (Exception e) {
                settingService.createSetting("PAYMENT-CREATION-DATE", "FNB-API", "");
                String creationDate = Conversion.dateToString(new Date());
                grpHdr.setCreationDateTime(creationDate);
            }
            grpHdr.setTotalNumberOfTransactions(1);
            grpHdr.setTotalControlSum(paymentRequestDto.getAmount().doubleValue());
            grpHdr.setInitiatingPartyName(settingService.getSetting("COMPANY-NAME", "TENANT"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return grpHdr;
    }

    private PaymentInformation paymentInformation(PaymentRequestDto paymentRequestDto) {
        PaymentInformation paymentInformation = new PaymentInformation();
        try {
            paymentInformation.setPaymentInformationId(paymentRequestDto.getNumber());
            paymentInformation.setPaymentInformationMethod("TRF");
            paymentInformation.setBatchBooking(false);
            paymentInformation.setNumberOfTransactions(1);
            paymentInformation.setControlSum(paymentRequestDto.getAmount().doubleValue());
            paymentInformation.setPaymentTypeInformationServiceLevelCode("SDVA");
            Instant instant = paymentRequestDto.getDueDate().toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);
            String isoDate = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
            if (paymentRequestDto.getDueDate().after(new Date())) {
                paymentInformation.setRequestedExecutionDate(Conversion.dateToString(paymentRequestDto.getDueDate()));
            } else {
                paymentInformation.setRequestedExecutionDate(Conversion.dateToString(new Date()));
            }
            Debtor debtor = new Debtor();
            debtor.setName(settingService.getSetting("ACCOUNT-HOLDER", "EFT-BANK-ACCOUNT"));
            debtor.setBicOrBEI(settingService.getSetting("BRANCH-CODE", "EFT-BANK-ACCOUNT"));
            paymentInformation.setDebtor(debtor);

            DebtorAccount debtorAccount = new DebtorAccount();
            debtorAccount.setAccountNumber(gcpTenantSecretService.resolveSetting("ACCOUNT-NUMBER", "EFT-BANK-ACCOUNT"));
            String accountType = settingService.getSetting("ACCOUNT-TYPE", "EFT-BANK-ACCOUNT");
            if (accountType.equals("CHEQUE")) {
                debtorAccount.setAccountType("CACC");
            } else if (accountType.equals("SAVINGS")) {
                debtorAccount.setAccountType("SVGS");
            }
            paymentInformation.setDebtorAccount(debtorAccount);
            DebtorAgent debtorAgent = new DebtorAgent();
            debtorAgent.setBranchId(settingService.getSetting("BRANCH-CODE", "EFT-BANK-ACCOUNT"));
            paymentInformation.setDebtorAgent(debtorAgent);

            CreditTransferTransactionInformation transactionInformation = new CreditTransferTransactionInformation();

            Amount amount = new Amount();
            amount.setCurrency(settingService.getSetting("CURRENCY", "TENANT"));
            amount.setValue(paymentRequestDto.getAmount().doubleValue());
            transactionInformation.setAmount(amount);

            BankAccountDto bankAccountDto;
            if (paymentRequestDto.getPaymentMethod().getCode().equals("EFT")) {
                bankAccountDto = bankAccountService.getList(paymentRequestDto.getId()).iterator().next();
                bankAccountDto.setBranchCode(bankAccountService.getUBC(bankAccountDto.getBankName().getCode()));
            } else {
                bankAccountDto = new BankAccountDto();
                bankAccountDto.setAccountHolder(settingService.getSetting("ACCOUNT-HOLDER", "CASH-BANK-ACCOUNT"));
                bankAccountDto.setBranchCode(settingService.getSetting("BRANCH-CODE", "CASH-BANK-ACCOUNT"));
                bankAccountDto.setAccountNumber(gcpTenantSecretService.resolveSetting("ACCOUNT-NUMBER", "CASH-BANK-ACCOUNT"));
                FieldOptionDto fieldOptionDto = new FieldOptionDto();
                fieldOptionDto.setCode(settingService.getSetting("ACCOUNT-TYPE", "CASH-BANK-ACCOUNT"));
                bankAccountDto.setAccountType(fieldOptionDto);
            }
            Creditor creditor = new Creditor();
            creditor.setName(bankAccountDto.getAccountHolder());
            creditor.setBicOrBEI(bankAccountDto.getBranchCode());
            transactionInformation.setCreditor(creditor);

            CreditorAccount creditorAccount = new CreditorAccount();
            creditorAccount.setAccountNumber(bankAccountDto.getAccountNumber());
            String creditAccountType = bankAccountDto.getAccountType().getCode();
            if (creditAccountType.equals("CHEQUE")) {
                creditorAccount.setAccountType("CACC");
            } else if (creditAccountType.equals("SAVINGS")) {
                creditorAccount.setAccountType("SVGS");
            }
            transactionInformation.setCreditorAccount(creditorAccount);

            CreditorAgent creditorAgent = new CreditorAgent();
            creditorAgent.setBranchId(bankAccountDto.getBranchCode());
            transactionInformation.setCreditorAgent(creditorAgent);

            String reference;
            List<PartnerIdentityDto> identityDtoArrayList
                    = partnerIdentityService.getByPartnerType(paymentRequestDto.getRecipient().getId(), "ACCOUNT-NUMBER");
            if (!identityDtoArrayList.isEmpty()) {
                reference = identityDtoArrayList.iterator().next().getNumber();
            } else {
                reference = paymentRequestDto.getReference();
            }

            String endToend = paymentRequestDto.getNumber() + paymentRequestDto.getPaymentReason().getDescription();
            String limited = endToend.length() > 30 ? endToend.substring(0, 30) : endToend;
            transactionInformation.setEndToEndId(limited);
            limited = reference.length() > 35 ? reference.substring(0, 35) : reference;
            transactionInformation.setRemittanceInformationUnstructured(limited);
            transactionInformation.setRemittanceLocationMethod("EMAL");
            transactionInformation.setRemittanceLocationElectronicAddress(gcpTenantSecretService.resolveSetting("POP-RECIPIENT", "FNB-API"));
            List<CreditTransferTransactionInformation> creditTransferTransactionInformationList = new ArrayList<>();
            creditTransferTransactionInformationList.add(transactionInformation);
            paymentInformation.setCreditTransferTransactionInformation(creditTransferTransactionInformationList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return paymentInformation;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getResponseCode() >= 200
                && connection.getResponseCode() < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

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
