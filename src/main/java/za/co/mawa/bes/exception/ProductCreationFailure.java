package za.co.mawa.bes.exception;

public class ProductCreationFailure extends Exception{
    public ProductCreationFailure() {
    }

    public ProductCreationFailure(String message) {
        super(message);
    }
}
