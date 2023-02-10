package za.co.mawa.bes.exception;

public class ProductDeleteFailure extends Exception{
    public ProductDeleteFailure() {
    }

    public ProductDeleteFailure(String message) {
        super(message);
    }
}
