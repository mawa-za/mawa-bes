package za.co.mawa.bes.exception;

public class DuplicateCreationException extends Exception{

    public DuplicateCreationException() {
    }

    public DuplicateCreationException(String message) {
        super(message);
    }
}
