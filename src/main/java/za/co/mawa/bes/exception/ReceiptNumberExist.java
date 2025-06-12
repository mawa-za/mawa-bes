package za.co.mawa.bes.exception;

public class ReceiptNumberExist extends Exception{
    public ReceiptNumberExist() {
    }

    public ReceiptNumberExist(String message) {
        super(message);
    }
}
