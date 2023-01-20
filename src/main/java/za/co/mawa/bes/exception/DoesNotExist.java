package za.co.mawa.bes.exception;

public class DoesNotExist extends Exception{
    public DoesNotExist() {
    }

    public DoesNotExist(String message) {
        super(message);
    }
}
