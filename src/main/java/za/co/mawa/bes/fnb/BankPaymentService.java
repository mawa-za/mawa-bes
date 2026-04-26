package za.co.mawa.bes.fnb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    PartnerIdentityService partnerIdentityService;

    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    TransactionService transactionService;

    private String getBaseURL() {
        return settingService.getSetting("BASE-URL", "FNB-API");
    }

    public String getToken() {
        try {

            URL url = new URL(getBaseURL() + "/oauth2/token/v2");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // If client credentials are required in Basic Auth header
            String clientId = settingService.getSetting("CLIENT-ID", "FNB-API");
            String clientSecret = settingService.getSetting("CLIENT-SECRET", "FNB-API");
            String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            conn.setRequestProperty("Authorization", "Basic " + basicAuth);

            // Body: grant_type, scope, etc.
            String data = "grant_type=client_credentials&scope=i_can";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes());
                os.flush();
            }

            // Read response
            int status = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()));

            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            OAuthTokenResponse tokenResponse = mapper.readValue(response.toString(), OAuthTokenResponse.class);

            return tokenResponse.getAccessToken();

        } catch (Exception e) {
            return "";
        }
    }


    public String sendPaymentRequest(String payload) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(getBaseURL() + "/paymentExecution/initiate/v1");
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + getToken());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 300) {
                InputStream stream;
                stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                ObjectMapper mapper = new ObjectMapper();
                BankPaymentResponse bankPaymentResponse = mapper.readValue(response.toString(), BankPaymentResponse.class);
                return bankPaymentResponse.getInstructionId();

            } else {
                String errorResponse = readErrorStream(connection);
                throw new IOException(String.format("Request failed with code: %d. Response: %s",
                        responseCode, errorResponse));
            }

        } catch (SocketTimeoutException e) {
            throw new IOException("Request timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException("Failed to send invoice request: " + e.getMessage(), e);
        } finally {
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
                String dateSetting = settingService.getSetting("PAYMENT-CREATION-DATE", "FNB-API");
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
            debtorAccount.setAccountNumber(settingService.getSetting("ACCOUNT-NUMBER", "EFT-BANK-ACCOUNT"));
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
                bankAccountDto.setAccountNumber(settingService.getSetting("ACCOUNT-NUMBER", "CASH-BANK-ACCOUNT"));
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
            transactionInformation.setRemittanceLocationElectronicAddress(settingService.getSetting("POP-RECIPIENT", "FNB-API"));
            List<CreditTransferTransactionInformation> creditTransferTransactionInformationList = new ArrayList<>();
            creditTransferTransactionInformationList.add(transactionInformation);
            paymentInformation.setCreditTransferTransactionInformation(creditTransferTransactionInformationList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return paymentInformation;
    }

    private static String readErrorStream(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
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
