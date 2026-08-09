package za.co.mawa.bes.configuration.context;

import java.util.Date;

public class UserContext {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserPartner = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> platformSession = new ThreadLocal<>();
    private static final ThreadLocal<String> platformUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> platformUsername = new ThreadLocal<>();
    private static final ThreadLocal<String> platformDisplayName = new ThreadLocal<>();
    private static final ThreadLocal<String> platformEmail = new ThreadLocal<>();
    private static final ThreadLocal<String> accountType = new ThreadLocal<>();
    private static final ThreadLocal<String> accessScope = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> testUser = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> protectedUser = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> externalTransactionsBlocked = new ThreadLocal<>();
    private static final ThreadLocal<Date> accessExpiresAt = new ThreadLocal<>();
    private static final ThreadLocal<String> handoffId = new ThreadLocal<>();
    private static final ThreadLocal<String> accessReason = new ThreadLocal<>();
    private static final ThreadLocal<String> ticketReference = new ThreadLocal<>();
    private static final ThreadLocal<String> handoffRoleId = new ThreadLocal<>();
    private static final ThreadLocal<String> handoffRoleDescription = new ThreadLocal<>();

    public static void setCurrentUser(String user) { currentUser.set(user); }
    public static String getCurrentUser() { return currentUser.get(); }
    public static void setCurrentUserPartner(String partner) { currentUserPartner.set(partner); }
    public static String getCurrentUserPartner() { return currentUserPartner.get(); }
    public static void setCurrentUserId(String userId) { currentUserId.set(userId); }
    public static String getCurrentUserId() { return currentUserId.get(); }
    public static void setPlatformSession(Boolean value) { platformSession.set(value); }
    public static Boolean isPlatformSession() { return Boolean.TRUE.equals(platformSession.get()); }
    public static void setPlatformUserId(String value) { platformUserId.set(value); }
    public static String getPlatformUserId() { return platformUserId.get(); }
    public static void setPlatformUsername(String value) { platformUsername.set(value); }
    public static String getPlatformUsername() { return platformUsername.get(); }
    public static void setPlatformDisplayName(String value) { platformDisplayName.set(value); }
    public static String getPlatformDisplayName() { return platformDisplayName.get(); }
    public static void setPlatformEmail(String value) { platformEmail.set(value); }
    public static String getPlatformEmail() { return platformEmail.get(); }
    public static void setAccountType(String value) { accountType.set(value); }
    public static String getAccountType() { return accountType.get(); }
    public static void setAccessScope(String value) { accessScope.set(value); }
    public static String getAccessScope() { return accessScope.get(); }
    public static void setTestUser(Boolean value) { testUser.set(value); }
    public static Boolean isTestUser() { return Boolean.TRUE.equals(testUser.get()); }
    public static void setProtectedUser(Boolean value) { protectedUser.set(value); }
    public static Boolean isProtectedUser() { return Boolean.TRUE.equals(protectedUser.get()); }
    public static void setExternalTransactionsBlocked(Boolean value) { externalTransactionsBlocked.set(value); }
    public static Boolean isExternalTransactionsBlocked() { return Boolean.TRUE.equals(externalTransactionsBlocked.get()); }
    public static void setAccessExpiresAt(Date value) { accessExpiresAt.set(value); }
    public static Date getAccessExpiresAt() { return accessExpiresAt.get(); }
    public static void setHandoffId(String value) { handoffId.set(value); }
    public static String getHandoffId() { return handoffId.get(); }
    public static void setAccessReason(String value) { accessReason.set(value); }
    public static String getAccessReason() { return accessReason.get(); }
    public static void setTicketReference(String value) { ticketReference.set(value); }
    public static String getTicketReference() { return ticketReference.get(); }
    public static void setHandoffRoleId(String value) { handoffRoleId.set(value); }
    public static String getHandoffRoleId() { return handoffRoleId.get(); }
    public static void setHandoffRoleDescription(String value) { handoffRoleDescription.set(value); }
    public static String getHandoffRoleDescription() { return handoffRoleDescription.get(); }

    public static void clear() {
        currentUser.remove(); currentUserPartner.remove(); currentUserId.remove();
        platformSession.remove(); platformUserId.remove(); platformUsername.remove(); platformDisplayName.remove();
        platformEmail.remove(); accountType.remove(); accessScope.remove(); testUser.remove(); protectedUser.remove();
        externalTransactionsBlocked.remove(); accessExpiresAt.remove(); handoffId.remove(); accessReason.remove(); ticketReference.remove();
        handoffRoleId.remove(); handoffRoleDescription.remove();
    }
}
