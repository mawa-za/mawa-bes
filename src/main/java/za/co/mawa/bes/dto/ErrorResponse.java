package za.co.mawa.bes.dto;

// ErrorResponse.java
public class ErrorResponse {
    private String message;
    private int status;
    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

}
