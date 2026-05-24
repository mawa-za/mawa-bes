package za.co.mawa.bes.configuration.context;

public class UserContext {
    private static ThreadLocal<String> currentUser = new ThreadLocal<String>();
    private static ThreadLocal<String> currentUserId = new ThreadLocal<String>();
    private static ThreadLocal<String> currentUserPartner = new ThreadLocal<String>();

    public static void setCurrentUser(String user) {
        currentUser.set(user);
    }
    public static String getCurrentUser() {
        return currentUser.get();
    }
    public static void setCurrentUserPartner(String partner) {
        currentUserPartner.set(partner);
    }
    public static String getCurrentUserPartner() {
        return currentUserPartner.get();
    }
    public static void setCurrentUserId(String userId) {
        currentUserId.set(userId);
    }
    public static String getCurrentUserId() {
        return currentUserId.get();
    }
    public static void clear() {
        currentUser.remove();
        currentUserPartner.remove();
    }
}
