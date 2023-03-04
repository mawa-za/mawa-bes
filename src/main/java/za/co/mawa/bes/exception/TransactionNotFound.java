package za.co.mawa.bes.exception;

public class TransactionNotFound extends Exception{
    public TransactionNotFound() {
    }

    public TransactionNotFound(String message) {
        super(message);
    }
}
