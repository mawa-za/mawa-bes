package za.co.mawa.bes.exception;

public class FieldDoesNotExist extends Exception{
    public FieldDoesNotExist() {
    }

    public FieldDoesNotExist(String message) {
        super(message);
    }
}
