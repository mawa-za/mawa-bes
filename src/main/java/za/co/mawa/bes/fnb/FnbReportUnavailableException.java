package za.co.mawa.bes.fnb;

import java.io.IOException;

/** A successful HTTP response without a report; bank payment outcome remains unknown. */
public class FnbReportUnavailableException extends IOException {
    public FnbReportUnavailableException(int statusCode) {
        super("FNB report response was empty (HTTP " + statusCode + "); payment outcome is not confirmed");
    }
}
