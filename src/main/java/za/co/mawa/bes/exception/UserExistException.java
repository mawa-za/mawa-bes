package za.co.mawa.bes.exception;

public class UserExistException extends Exception{
    public UserExistException() {
    }

    public UserExistException(String message) {
        super(message);
    }
}
