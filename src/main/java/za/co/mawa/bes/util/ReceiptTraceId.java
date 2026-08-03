package za.co.mawa.bes.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Creates the stable public trace identifier printed on MAWA receipts.
 * The same receipt number always resolves to the same trace identifier, which
 * allows MawaPay to print it while offline and the backend to verify it later.
 */
public final class ReceiptTraceId {

    private static final String PREFIX = "MPR";

    private ReceiptTraceId() {
    }

    public static String fromReceiptNo(String receiptNo) {
        String normalized = receiptNo == null ? "" : receiptNo.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("receiptNo is required to create a receipt trace ID");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format(Locale.ROOT, "%02X", digest[i]));
            }
            return PREFIX + "-"
                    + hex.substring(0, 4) + "-"
                    + hex.substring(4, 8) + "-"
                    + hex.substring(8, 12) + "-"
                    + hex.substring(12, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
