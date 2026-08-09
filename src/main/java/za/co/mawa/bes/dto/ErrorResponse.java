package za.co.mawa.bes.dto;

/** Stable error envelope consumed by MAWA frontends. */
public class ErrorResponse {
    private final String message;
    private final int status;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
