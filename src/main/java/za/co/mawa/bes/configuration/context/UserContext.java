package za.co.mawa.bes.configuration.context;

public class UserContext {
    private static ThreadLocal<String> currentUser = new ThreadLocal<String>();

    public static void setCurrentUser(String user) {
        currentUser.set(user);
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
