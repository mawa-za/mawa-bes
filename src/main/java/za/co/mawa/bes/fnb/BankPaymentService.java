package za.co.mawa.bes.fnb;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.fnb.dto.*;
import za.co.mawa.bes.service.PartnerIdentityService;
import za.co.mawa.bes.service.PaymentRequestService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.utils.Conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class BankPaymentService {
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    SettingService settingService;
    @Autowired
    PartnerIdentityService partnerIdentityService;

    private String getBaseURL(){
        return settingService.getSetting("BASE-URL", "FNB-API");
    }
    public String getToken() {
        try {

            URL url = new URL(getBaseURL()+"/oauth/token");
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
            String data = "grant_type=client_credentials&scope=read";

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

            return response.toString();

        } catch (Exception e) {
            return "";
        }
    }


    public void sendPaymentRequest(JSONObject requestBody, String token ) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(getBaseURL()+"/paymentExecution/initiate/v1");
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
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

    public BankPaymentRequest generateRequest(String paymentResuestId) {
        BankPaymentRequest bankPaymentRequest = new BankPaymentRequest();
        try {
            PaymentRequestDto paymentRequestDto = paymentRequestService.get(paymentResuestId);
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
            grpHdr.setMessageId(paymentRequestDto.getId());
            grpHdr.setCreationDateTime(new Date().toLocaleString());
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
            paymentInformation.setPaymentInformationId(paymentRequestDto.getId());
            paymentInformation.setPaymentInformationMethod("TRF");
            paymentInformation.setBatchBooking(false);
            paymentInformation.setNumberOfTransactions(1);
            paymentInformation.setControlSum(paymentRequestDto.getAmount().doubleValue());
            paymentInformation.setPaymentTypeInformationServiceLevelCode("SDVA");
            paymentInformation.setRequestedExecutionDate(paymentRequestDto.getDueDate().toLocaleString());

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

            DebtorAgent debtorAgent = new DebtorAgent();
            debtorAgent.setBranchId(settingService.getSetting("BRANCH-CODE", "EFT-BANK-ACCOUNT"));

            CreditTransferTransactionInformation transactionInformation = new CreditTransferTransactionInformation();

            Amount amount = new Amount();
            amount.setCurrency(settingService.getSetting("CURRENCY", "TENANT"));
            amount.setValue(paymentRequestDto.getAmount().doubleValue());
            transactionInformation.setAmount(amount);

            Creditor creditor = new Creditor();
            creditor.setName(paymentRequestDto.getBankAccount().getAccountHolder());
            creditor.setBicOrBEI(paymentRequestDto.getBankAccount().getBranchCode());
            transactionInformation.setCreditor(creditor);

            CreditorAccount creditorAccount = new CreditorAccount();
            creditorAccount.setAccountNumber(paymentRequestDto.getBankAccount().getAccountNumber());
            String creditAccountType = paymentRequestDto.getBankAccount().getAccountType().getCode();
            if (creditAccountType.equals("CHEQUE")) {
                debtorAccount.setAccountType("CACC");
            } else if (creditAccountType.equals("SAVINGS")) {
                debtorAccount.setAccountType("SVGS");
            }
            transactionInformation.setCreditorAccount(creditorAccount);

            CreditorAgent creditorAgent = new CreditorAgent();
            creditorAgent.setBranchId(paymentRequestDto.getBankAccount().getBranchCode());
            transactionInformation.setCreditorAgent(creditorAgent);

            String reference;
            List<PartnerIdentityDto> identityDtoArrayList
                    = partnerIdentityService.getByPartnerType(paymentRequestDto.getRecipient().getId(), "ACCOUNT-NUMBER");
            if (!identityDtoArrayList.isEmpty()) {
                reference = identityDtoArrayList.iterator().next().getNumber();
            } else {
                reference = paymentRequestDto.getReference();
            }
            String endToend = (paymentRequestDto.getNumber() + paymentRequestDto.getPaymentReason().getDescription()).substring(0,30);
            transactionInformation.setEndToEndId(endToend);
            transactionInformation.setRemittanceInformationUnstructured(reference.substring(0,35));
            transactionInformation.setRemittanceLocationMethod("EMAL");
            transactionInformation.setRemittanceLocationElectronicAddress(settingService.getSetting("POP-RECIPIENT", "BANK-PAYMENT"));
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

}
