package za.co.mawa.bes.configuration.context;

public abstract class TenantContext {
    public static final String LOCALHOST_HOST = "localhost";
    public static final String DEFAULT_TENANT_ID = "mawa";

    private static ThreadLocal<String> currentTenant = new ThreadLocal<String>();

    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }

}