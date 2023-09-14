package za.co.mawa.bes.exception;

public class TenantNotFound extends Exception{
    public TenantNotFound() {
    }

    public TenantNotFound(String message) {
        super(message);
    }
}
