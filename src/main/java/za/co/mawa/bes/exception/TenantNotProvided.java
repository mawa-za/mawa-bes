package za.co.mawa.bes.exception;

public class TenantNotProvided extends Exception{
    public TenantNotProvided() {
    }

    public TenantNotProvided(String message) {
        super(message);
    }
}
