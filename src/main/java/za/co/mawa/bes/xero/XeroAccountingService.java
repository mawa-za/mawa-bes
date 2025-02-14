package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class XeroAccountingService {

    private static String INVOICE_URL = "https://api.xero.com/api.xro/2.0/Invoices";


    public String createInvoice(String accessToken , String XeroTenantId){
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Create root node
            ObjectNode invoice = objectMapper.createObjectNode();
            invoice.put("Type", "ACCREC");

            // Create Contact node
            ObjectNode contact = objectMapper.createObjectNode();
            contact.put("ContactID", "3786a1f7-d899-4dc5-a0e9-cc972ae89299");
            invoice.set("Contact", contact);

            // Add Dates
            invoice.put("Date", "2019-03-11");
            invoice.put("DueDate", "2018-12-10");

            // Reference & Status
            invoice.put("Reference", "Website Design");
            invoice.put("Status", "AUTHORISED");

            // Create LineItems array
            ArrayNode lineItems = objectMapper.createArrayNode();
            ObjectNode lineItem = objectMapper.createObjectNode();
            lineItem.put("Description", "Acme Tires");
            lineItem.put("Quantity", 2);
            lineItem.put("UnitAmount", 20);
            lineItem.put("AccountCode", "200");
            lineItem.put("TaxType", "NONE");
            lineItem.put("LineAmount", 40);
            lineItems.add(lineItem);

            invoice.set("LineItems", lineItems);

            // Wrap inside the Invoices array
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode invoices = objectMapper.createArrayNode();
            invoices.add(invoice);
            root.set("Invoices", invoices);

            // Convert to JSON String
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            JSONObject invoiceJson = new JSONObject(jsonString);
//            String XeroTenantId = getXeroTenantId(accessToken);
            System.out.println("tenant id " + XeroTenantId);
            return sendInvoiceRequest(invoiceJson , accessToken ,XeroTenantId );

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    public String sendInvoiceRequest(JSONObject requestBody, String accessToken , String xero_tenant_id) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(INVOICE_URL);
            connection = (HttpURLConnection) url.openConnection();

            // Configure connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Xero-Tenant-Id", xero_tenant_id);
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

            return response.toString(); // Return the full response;

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

    // Helper method to read error stream
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
