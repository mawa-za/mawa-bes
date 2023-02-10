package za.co.mawa.bes.exception;

public class RoleDoesNotExist extends Exception{
    public RoleDoesNotExist() {
    }

    public RoleDoesNotExist(String message) {
        super(message);
    }
}
