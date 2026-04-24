package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xero.api.client.AccountingApi;
import com.xero.models.accounting.Invoice;
import com.xero.models.accounting.Invoices;
import com.xero.models.accounting.LineItem;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.invoice.InvoiceOutboundDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class XeroAccountingService {

    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    XeroAuthService xeroAuthService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    AccountingApi accountingApi;

    private static String INVOICE_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private static String CONTACT_URL = "https://api.xero.com/api.xro/2.0/Contacts";


    public InvoiceOutboundDto createInvoice(String partnerId , String reference , String itemCode){
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

    public InvoiceOutboundDto createFuneralInvoice(String partnerId , String reference , String itemCode){
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

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 1);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","HEARSE");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 2);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","FAMCAR7");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 1);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","CORPSE-COLLECTION");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 1);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","STORAGE");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 1);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","STANDARD-TENT");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 100);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","STANDARD-CHAIR");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 24);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","WATER-500ML");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 24);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","COOL-DRINK-330ML");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 24);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","JUICE-330ML");
            lineItems.add(lineItem);

            lineItem = objectMapper.createObjectNode();
            lineItem.put("Quantity", 1);
            lineItem.put("UnitAmount", 0);
            lineItem.put("ItemCode","DECORATION");
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

    public InvoiceOutboundDto sendInvoiceRequest(JSONObject requestBody, String accessToken , String xero_tenant_id) throws IOException {
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

    public InvoiceOutboundDto addLineItemToInvoice(UUID invoiceId, LineItem lineItem) throws Exception {
        String tenant = xeroAuthService.checkXeroInfo();
        String accessToken = xeroAuthService.refreshAccessToken(tenant);
        String tenantProperty = tenantAdminService.getTenantProperty(tenant);
        JSONObject jsonObject = new JSONObject(tenantProperty);
        String XeroTenantId = jsonObject.getString("XERO-TENANT-ID");
        // Fetch existing invoice
        Invoice invoiceToUpdate = accountingApi.getInvoice(accessToken, XeroTenantId, invoiceId,2).getInvoices().get(0);

        // Create new line item
//        LineItem newLineItem = new LineItem()
//                .description("Additional service")
//                .quantity(BigDecimal.valueOf(1).doubleValue())
//                .unitAmount(BigDecimal.valueOf(50).doubleValue())
//                .accountCode("200"); // Use a valid revenue account code

        // Add new line to existing lines
        List<LineItem> updatedLines = new ArrayList<>(invoiceToUpdate.getLineItems());
        updatedLines.add(lineItem);
        invoiceToUpdate.setLineItems(updatedLines);
        Invoices invoices = new Invoices();
        Invoices invoicesWrapper = new Invoices()
                .invoices(Collections.singletonList(invoiceToUpdate));

        Invoices updatedInvoices = accountingApi.updateInvoice(
                accessToken,
                XeroTenantId,
                invoiceToUpdate.getInvoiceID(),
                invoicesWrapper,
                2,
                UUID.randomUUID().toString()
        );

        Invoice updatedInvoice = updatedInvoices.getInvoices().get(0);

        InvoiceOutboundDto invoiceOutboundDto = new InvoiceOutboundDto();
        invoiceOutboundDto.setNumber(updatedInvoice.getInvoiceNumber());
        invoiceOutboundDto.setId(updatedInvoice.getInvoiceID().toString());

        return invoiceOutboundDto;
    }
}
