package za.co.mawa.bes.xero;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Canonical Xero Accounting API URLs; tenant properties must not redirect bearer tokens. */
final class XeroAccountingEndpoint {
    static final String ROOT = "https://api.xero.com/api.xro/2.0";
    private static final Set<String> RESOURCES = Set.of("Invoices", "Payments", "CreditNotes", "Contacts", "Items", "Accounts", "TaxRates");
    private XeroAccountingEndpoint() {}

    static String resolve(String configured, String resource) {
        if (!RESOURCES.contains(resource)) throw new IllegalArgumentException("Unsupported Xero resource");
        String target = ROOT + "/" + resource;
        if (configured == null || configured.isBlank()) return target;
        String value = configured.trim().replaceAll("/+$", "");
        if (value.equals(ROOT) || value.equals(target)) return target;
        throw new IllegalArgumentException("Invalid Xero endpoint configuration for " + resource + "; use " + ROOT + " or " + target);
    }

    static String readBody(HttpURLConnection connection, int status) throws IOException {
        InputStream stream = status >= 300 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) return "";
        try (stream) { return new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
    }
}
