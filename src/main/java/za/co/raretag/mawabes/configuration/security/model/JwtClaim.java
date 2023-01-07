package za.co.raretag.mawabes.configuration.security.model;

public enum JwtClaim {

    TOKEN_ID("tenant-id");

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