package za.co.mawa.bes.exception;

/**
 * Raised when MAWA cannot create the printable claim form required during
 * claim submission. The technical cause is retained for server diagnostics,
 * while the API exception handler returns a safe, actionable message.
 */
public class ClaimFormGenerationException extends RuntimeException {

    public ClaimFormGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
