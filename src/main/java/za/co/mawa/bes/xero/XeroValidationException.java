package za.co.mawa.bes.xero;

import java.io.IOException;

/** Explicit HTTP 400 response: a rejected write requires correction rather than automatic retries. */
public class XeroValidationException extends IOException {
    public XeroValidationException(String message) { super(message); }
}
