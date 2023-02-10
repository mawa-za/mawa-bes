package za.co.mawa.bes.exception;

public class ProductUpdateFailure extends Exception{
    public ProductUpdateFailure() {
    }

    public ProductUpdateFailure(String message) {
        super(message);
    }
}
