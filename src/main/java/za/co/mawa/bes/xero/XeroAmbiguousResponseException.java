package za.co.mawa.bes.xero;

import java.io.IOException;

/** A write request was transmitted but its outcome could not be confirmed. */
public class XeroAmbiguousResponseException extends IOException {
    public XeroAmbiguousResponseException(String message) {
        super(message);
    }

    public XeroAmbiguousResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
