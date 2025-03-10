package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class XeroAccountingService {

    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    XeroAuthService xeroAuthService;
    @Autowired
    TenantAdminService tenantAdminService;
    Gson gson = new Gson();

    @Value("${mawa.api.url}")
    private String authUrl;
    private static String INVOICE_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private static String CONTACT_URL = "https://api.xero.com/api.xro/2.0/Contacts";


    public String createInvoice(String partnerId , String reference , String itemCode){
        try {
            //get accessToken and XeroTenantId
            //use the partner id to get the contact id from partner
            //if authentication fails, refresh token
            //schedule function to refresh refresh token after 30 days

            //get this tenant from the partnerId
//            String tenant = TenantContext.getCurrentTenant();
//            String tenant = getContactIdFromPartner(partnerId);
            String tenant = xeroAuthService.checkXeroInfo();
            String accessToken = xeroAuthService.refreshAccessToken(tenant);

            String tenantProperty = tenantAdminService.getTenantProperty(tenant);
            JSONObject jsonObject = new JSONObject(tenantProperty);
            String XeroTenantId = jsonObject.getString("XERO-TENANT-ID");

            ObjectMapper objectMapper = new ObjectMapper();

            // Create root node
            ObjectNode invoice = objectMapper.createObjectNode();
            invoice.put("Type", "ACCREC");

            // Create Contact node
            ObjectNode contact = objectMapper.createObjectNode();
            contact.put("ContactID", getContactIdFromPartner(partnerId));
//            contact.put("ContactID", "3786a1f7-d899-4dc5-a0e9-cc972ae89299");
            invoice.set("Contact", contact);

            // Add Dates
//            invoice.put("Date", "2019-03-11");
//            invoice.put("DueDate", "2018-12-10");

            // Reference & Status
            invoice.put("Reference", reference);
//            invoice.put("Status", "AUTHORISED");

            // Create LineItems array
            ArrayNode lineItems = objectMapper.createArrayNode();
            ObjectNode lineItem = objectMapper.createObjectNode();
//            lineItem.put("Description", "Acme Tires");
            lineItem.put("Quantity", 1);
//            lineItem.put("UnitAmount", 20);
//            lineItem.put("AccountCode", "200");
            lineItem.put("ItemCode",itemCode);
//            lineItem.put("TaxType", "NONE");
//            lineItem.put("LineAmount", 40);
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
//            System.out.println("tenant id " + XeroTenantId);
            return sendInvoiceRequest(invoiceJson , accessToken ,XeroTenantId );

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    public ObjectNode getXeroContact() throws IOException {

        String tenant = xeroAuthService.checkXeroInfo();
        String accessToken = xeroAuthService.refreshAccessToken(tenant);
        String tenantProperty = tenantAdminService.getTenantProperty(tenant);
        JSONObject jsonObject = new JSONObject(tenantProperty);
        String XeroTenantId = jsonObject.getString("XERO-TENANT-ID");

        return sendContactsRequest(accessToken,XeroTenantId);
    }

    public ObjectNode sendContactsRequest(String accessToken, String xeroTenantId) throws IOException {
        HttpURLConnection connection = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            URL url = new URL(CONTACT_URL);
            connection = (HttpURLConnection) url.openConnection();

            // Configure connection
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Xero-Tenant-Id", xeroTenantId);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Read and handle response
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
                String errorResponse = readErrorStream(connection);
                throw new IOException(String.format("Request failed with code: %d. Response: %s",
                        responseCode, errorResponse));
            }

            // Read successful response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Parse response and transform structure
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            ObjectNode formattedResponse = objectMapper.createObjectNode();
            formattedResponse.put("Id", jsonResponse.path("Id").asText());
            formattedResponse.put("Status", jsonResponse.path("Status").asText());
            formattedResponse.put("ProviderName", jsonResponse.path("ProviderName").asText());
            formattedResponse.put("DateTimeUTC", jsonResponse.path("DateTimeUTC").asText());

            ArrayNode formattedContacts = objectMapper.createArrayNode();
            for (JsonNode contact : jsonResponse.path("Contacts")) {
                ObjectNode formattedContact = objectMapper.createObjectNode();
                formattedContact.put("ContactID", contact.path("ContactID").asText());
                formattedContact.put("ContactStatus", contact.path("ContactStatus").asText());
                formattedContact.put("Name", contact.path("Name").asText());

                // Extract Addresses
                ArrayNode formattedAddresses = objectMapper.createArrayNode();
                for (JsonNode address : contact.path("Addresses")) {
                    ObjectNode formattedAddress = objectMapper.createObjectNode();
                    formattedAddress.put("AddressType", address.path("AddressType").asText());
                    formattedAddresses.add(formattedAddress);
                }
                formattedContact.set("Addresses", formattedAddresses);

                // Extract Phones
                ArrayNode formattedPhones = objectMapper.createArrayNode();
                for (JsonNode phone : contact.path("Phones")) {
                    ObjectNode formattedPhone = objectMapper.createObjectNode();
                    formattedPhone.put("PhoneType", phone.path("PhoneType").asText());
                    formattedPhones.add(formattedPhone);
                }
                formattedContact.set("Phones", formattedPhones);

                formattedContact.put("UpdatedDateUTC", contact.path("UpdatedDateUTC").asText());
                formattedContact.set("ContactGroups", contact.path("ContactGroups"));
                formattedContact.put("IsSupplier", contact.path("IsSupplier").asBoolean());
                formattedContact.put("IsCustomer", contact.path("IsCustomer").asBoolean());
                formattedContact.set("ContactPersons", contact.path("ContactPersons"));
                formattedContact.put("HasAttachments", contact.path("HasAttachments").asBoolean());
                formattedContact.put("HasValidationErrors", contact.path("HasValidationErrors").asBoolean());

                formattedContacts.add(formattedContact);
            }
            formattedResponse.set("Contacts", formattedContacts);

            return formattedResponse;
        } catch (SocketTimeoutException e) {
            throw new IOException("Request timed out: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException("Failed to get Xero contacts: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

            // Convert response to JSON object
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray invoices = jsonResponse.getJSONArray("Invoices");
            JSONObject firstInvoice = invoices.getJSONObject(0);
            String invoiceNumber = firstInvoice.getString("InvoiceNumber");
            return invoiceNumber;

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

    private String getContactIdFromPartner(String partnerId){
        List<PartnerIdentityEntity> identityList = partnerIdentityRepository.findPartnerIdentityByPartner(partnerId);

        for (PartnerIdentityEntity partnerIdentityEntity : identityList) {
            if (XeroUtils.XERO_CONTACT_ID.equals(partnerIdentityEntity.getPartnerIdentityPK().getType())) {
                return partnerIdentityEntity.getPartnerIdentityPK().getValue();
            }
        }
        return null;
    }

    public String createExternalLogin(XeroInboundInvoiceCreateDto xeroInboundInvoiceCreateDto, String tenant) {
        JwtRequest tokenRequest = new JwtRequest();
        tokenRequest.setUsername(xeroInboundInvoiceCreateDto.getUsername());
        tokenRequest.setPassword(xeroInboundInvoiceCreateDto.getPassword());

        String token = "";
        try {
            URL url = new URL(authUrl + "/authenticate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-TenantID", tenant);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(gson.toJson(tokenRequest));
            writer.close();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();

            // Parse the JSON response to extract the token
            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            token = jsonObject.get("token").getAsString();

        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return token;
    }

    public String createExternalInvoice(XeroInboundInvoiceCreateDto xeroInboundInvoiceCreateDto , String tenant){

        String token = createExternalLogin(xeroInboundInvoiceCreateDto,tenant);

        try {
//            String URI = "http://localhost:8080";

            URL url = new URL(authUrl + "/xero/createInvoice");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-TenantID", tenant);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(gson.toJson(xeroInboundInvoiceCreateDto));
            writer.close();
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode() );
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            conn.disconnect();
            return response.toString();
        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return null;
    }
}
