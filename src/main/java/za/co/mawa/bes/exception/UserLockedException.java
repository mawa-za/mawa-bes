package za.co.mawa.bes.exception;

public class UserLockedException extends Exception{
    public UserLockedException() {
    }

    public UserLockedException(String message) {
        super(message);
    }
}
