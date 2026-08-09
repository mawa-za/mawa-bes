package za.co.mawa.bes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReceiptTraceIdTest {

    @Test
    void createsStableCaseInsensitiveTraceId() {
        assertEquals(
                "MPR-8253-4465-C978-CF67",
                ReceiptTraceId.fromReceiptNo(" rcpt-001 ")
        );
        assertEquals(
                ReceiptTraceId.fromReceiptNo("RCPT-001"),
                ReceiptTraceId.fromReceiptNo("rcpt-001")
        );
    }

    @Test
    void rejectsBlankReceiptNumber() {
        assertThrows(IllegalArgumentException.class, () -> ReceiptTraceId.fromReceiptNo("  "));
    }
}
