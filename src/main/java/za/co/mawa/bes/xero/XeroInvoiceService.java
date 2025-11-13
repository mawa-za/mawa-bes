package za.co.mawa.bes.xero;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.invoice.InvoiceOutboundDto;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class XeroInvoiceService {

    @Autowired
    XeroAuthService xeroAuthService;
    @Autowired
    TenantAdminService tenantAdminService;

//    public XeroInvoiceDto getInvoice(String invoiceId) {
//        String tenantId = xeroAuthService.checkXeroInfo();
//        String accessToken = null;
//        try {
//            accessToken = xeroAuthService.refreshAccessToken(tenantId);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        // If your Xero response wraps invoices, adjust accordingly.
//        return xeroWebClient.get()
//                .uri("/Invoices/{invoiceId}", invoiceId)
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
//                .header("Xero-tenant-id", tenantId)
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(XeroInvoiceDto.class)  // or wrapper type
//                .block();
//    }


    public InvoiceOutboundDto sendInvoiceRequest(JSONObject requestBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            String tenant = xeroAuthService.checkXeroInfo();
            String accessToken = xeroAuthService.refreshAccessToken(tenant);
            String tenantProperty = tenantAdminService.getTenantProperty(tenant);
            JSONObject jsonObject = new JSONObject(tenantProperty);
            String XeroTenantId = jsonObject.getString("XERO-TENANT-ID");
            String XeroBaseURL = jsonObject.getString("XERO-BASE-URL");

            URL url = new URL(XeroBaseURL);
            connection = (HttpURLConnection) url.openConnection();

            // Configure connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Xero-Tenant-Id", XeroTenantId);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // Read and handle response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
                String errorResponse = readErrorStream(connection);
                throw new IOException(String.format("Request failed with code: %d. Response: %s",
                        responseCode, errorResponse));
            }

            // Read successful response if needed
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Convert response to JSON object
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray invoices = jsonResponse.getJSONArray("Invoices");
            JSONObject firstInvoice = invoices.getJSONObject(0);
            String invoiceNumber = firstInvoice.getString("InvoiceNumber");
            String invoiceID = firstInvoice.getString("InvoiceID");
            InvoiceOutboundDto invoiceOutboundDto = new InvoiceOutboundDto();
            invoiceOutboundDto.setNumber(invoiceNumber);
            invoiceOutboundDto.setId(invoiceID);
            return invoiceOutboundDto;

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

    private String readErrorStream(HttpURLConnection connection) throws IOException {
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

