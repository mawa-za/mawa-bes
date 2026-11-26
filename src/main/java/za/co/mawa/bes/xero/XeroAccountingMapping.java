package za.co.mawa.bes.xero;

import org.json.JSONObject;

/** Validated selection, bound to the exact Xero organisation and account IDs. */
public record XeroAccountingMapping(String organisationId, String invoiceAccountId,
                                    String invoiceAccountCode, String paymentAccountId,
                                    String paymentAccountCode, String invoiceTaxType) {
    public static final String ATTRIBUTE = "ACCOUNTING-MAPPING";

    public static XeroAccountingMapping parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            JSONObject json = new JSONObject(value);
            var mapping = new XeroAccountingMapping(json.getString("organisationId"),
                    json.getString("invoiceAccountId"), json.getString("invoiceAccountCode"),
                    json.getString("paymentAccountId"), json.getString("paymentAccountCode"),
                    json.getString("invoiceTaxType"));
            return mapping.complete() ? mapping : null;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public String serialise() {
        return new JSONObject().put("organisationId", organisationId)
                .put("invoiceAccountId", invoiceAccountId).put("invoiceAccountCode", invoiceAccountCode)
                .put("paymentAccountId", paymentAccountId).put("paymentAccountCode", paymentAccountCode)
                .put("invoiceTaxType", invoiceTaxType).toString();
    }

    public boolean complete() {
        return text(organisationId) && text(invoiceAccountId) && text(invoiceAccountCode)
                && text(paymentAccountId) && text(paymentAccountCode) && text(invoiceTaxType);
    }

    public boolean matchesOrganisation(String selectedId) {
        return text(selectedId) && organisationId != null && organisationId.equalsIgnoreCase(selectedId);
    }

    private static boolean text(String value) { return value != null && !value.isBlank(); }
}
