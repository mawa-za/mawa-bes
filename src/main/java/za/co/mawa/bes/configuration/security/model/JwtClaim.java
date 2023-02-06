package za.co.mawa.bes.configuration.security.model;

public enum JwtClaim {

    TENANT_ID("tenant-id");

    private String value;

    private JwtClaim(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return this.name();
    }
}