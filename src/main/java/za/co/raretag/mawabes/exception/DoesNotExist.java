package za.co.raretag.mawabes.exception;

public class DoesNotExist extends Exception{
    public DoesNotExist() {
    }

    public DoesNotExist(String message) {
        super(message);
    }
}
