package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class XeroMasterDataPushService {
    private static final String CONTACTS_URL = "https://api.xero.com/api.xro/2.0/Contacts";
    private static final String ITEMS_URL = "https://api.xero.com/api.xro/2.0/Items";
    private final PartnerRepository partnerRepository;
    private final PartnerContactRepository contactRepository;
    private final ProductRepository productRepository;
    private final XeroAuthService authService;
    private final XeroIntegrationSettingsService settings;
    private final ObjectMapper mapper = new ObjectMapper();

    public XeroMasterDataPushService(PartnerRepository partnerRepository,
                                     PartnerContactRepository contactRepository,
                                     ProductRepository productRepository,
                                     XeroAuthService authService,
                                     XeroIntegrationSettingsService settings) {
        this.partnerRepository = partnerRepository;
        this.contactRepository = contactRepository;
        this.productRepository = productRepository;
        this.authService = authService;
        this.settings = settings;
    }

    public void pushCustomer(String partnerId) throws IOException {
        if (!settings.isIntegrationEnabled()) return;
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));
        ObjectNode contact = mapper.createObjectNode();
        if (text(partner.getXeroContactId())) contact.put("ContactID", partner.getXeroContactId());
        contact.put("ContactNumber", first(partner.getNo(), partner.getId()));
        contact.put("Name", limit(partnerName(partner), 255));
        ArrayNode phones = mapper.createArrayNode();
        for (PartnerContactEntity value : contactRepository.findContactsByPartner(partnerId)) {
            if (value.getPartnerContactPK() == null || !text(value.getValue())) continue;
            String type = value.getPartnerContactPK().getType().toUpperCase();
            if (type.contains("EMAIL")) contact.put("EmailAddress", value.getValue());
            else {
                ObjectNode phone = mapper.createObjectNode();
                phone.put("PhoneType", type.contains("ALTERNATIVE") ? "DEFAULT" : "MOBILE");
                phone.put("PhoneNumber", value.getValue());
                phones.add(phone);
            }
        }
        if (!phones.isEmpty()) contact.set("Phones", phones);
        ObjectNode root = mapper.createObjectNode();
        root.set("Contacts", mapper.createArrayNode().add(contact));
        JsonNode response = post(resolveUrl("XERO-CONTACTS-URL", CONTACTS_URL), root);
        JsonNode saved = firstResult(response, "Contacts");
        String id = saved.path("ContactID").asText(null);
        if (!text(id)) throw new IOException("Xero customer response did not contain ContactID");
        partner.setXeroContactId(id);
        partnerRepository.save(partner);
    }

    public void pushProduct(String productId) throws IOException {
        if (!settings.isIntegrationEnabled()) return;
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!text(product.getCode())) throw new IllegalArgumentException("Product code is required for Xero sync");
        ObjectNode item = mapper.createObjectNode();
        if (text(product.getXeroItemId())) item.put("ItemID", product.getXeroItemId());
        item.put("Code", product.getCode());
        item.put("Name", limit(first(product.getDescription(), product.getCode()), 50));
        item.put("Description", limit(first(product.getDescription(), product.getCode()), 4000));
        item.put("IsSold", Boolean.TRUE.equals(product.getAvailableForSale()));
        ObjectNode root = mapper.createObjectNode();
        root.set("Items", mapper.createArrayNode().add(item));
        JsonNode response = post(resolveUrl("XERO-ITEMS-URL", ITEMS_URL), root);
        JsonNode saved = firstResult(response, "Items");
        String id = saved.path("ItemID").asText(null);
        if (!text(id)) throw new IOException("Xero product response did not contain ItemID");
        product.setXeroItemId(id);
        productRepository.save(product);
    }

    private JsonNode post(String endpoint, ObjectNode payload) throws IOException {
        String tenant = authService.checkXeroInfo();
        String token = authService.refreshAccessToken(tenant);
        String xeroTenant = authService.getXeroProperty(tenant, XeroUtils.XERO_TENANT_ID);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Xero-Tenant-Id", xeroTenant);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) { output.write(mapper.writeValueAsBytes(payload)); }
            int code = connection.getResponseCode();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 300 && connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream(),
                    StandardCharsets.UTF_8))) {
                String body = reader.lines().reduce("", String::concat);
                if (code >= 300) throw new IOException("Xero master-data request failed (" + code + "): " + body);
                return mapper.readTree(body);
            }
        } finally { connection.disconnect(); }
    }

    private String resolveUrl(String key, String fallback) {
        String tenant = authService.checkXeroInfo();
        return authService.getXeroProperty(tenant, key, fallback);
    }
    private JsonNode firstResult(JsonNode response, String field) {
        JsonNode values = response.path(field);
        return values.isArray() && !values.isEmpty() ? values.get(0) : mapper.createObjectNode();
    }
    private String partnerName(PartnerEntity p) {
        String value = String.join(" ", empty(p.getName2()), empty(p.getName3()), empty(p.getName1())).trim();
        return first(value, p.getNo(), p.getId());
    }
    private String first(String... values) { for (String value : values) if (text(value)) return value.trim(); return ""; }
    private String empty(String value) { return value == null ? "" : value; }
    private String limit(String value, int length) { return value.length() <= length ? value : value.substring(0, length); }
    private boolean text(String value) { return value != null && !value.trim().isEmpty(); }
}
