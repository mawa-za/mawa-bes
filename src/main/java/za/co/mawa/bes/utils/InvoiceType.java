package za.co.mawa.bes.utils;

public enum InvoiceType {
    SALES_INVOICE("SALES-INVOICE"),
    APPOINTMENT("APPOINTMENT");

    private final String value;

    InvoiceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static InvoiceType fromString(String value) {
        for (InvoiceType type : InvoiceType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown invoice type: " + value);
    }
}
