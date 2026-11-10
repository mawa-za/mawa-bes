package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BankStatementCsvParserTest {
    private final BankStatementCsvParser parser = new BankStatementCsvParser();

    @Test
    void parsesRealisticFnbHistoryIncludingDebitsAndQuotedReferences() {
        String input = "ACCOUNT TRANSACTION HISTORY,,,,,,\n"
                + "FOR ACCOUNT NUMBER00000012345678,,,,,,\n"
                + "DATE,SERVICE FEE,AMOUNT,REFERENCE,CHEQUE NUMBER,BALANCE,\n"
                + "2026-11-05,0.00,15460.00,CLIENT EFT,0,-103559.50,\n"
                + "2026-11-05,0.00,-1900.00,CLAIM PAYOUT,0,-105459.50,\n"
                + "2026-11-06,0.00,250.50,\"ACME, INV 101\",0,-105209.00,\n";
        var rows = parser.parse(input.getBytes(StandardCharsets.UTF_8));
        assertEquals(3, rows.size());
        assertEquals("00000012345678", rows.get(0).accountNumber());
        assertEquals(1546000L, rows.get(0).amountCents());
        assertEquals(-190000L, rows.get(1).amountCents());
        assertEquals("ACME, INV 101", rows.get(2).reference());
        assertEquals(-10520900L, rows.get(2).balanceCents());
    }

    @Test
    void fingerprintsRepeatDeterministicallyButKeepGenuineDuplicateRows() {
        String input = "FOR ACCOUNT NUMBER1234\nDATE,SERVICE FEE,AMOUNT,REFERENCE,CHEQUE NUMBER,BALANCE\n"
                + "2026-11-05,0.00,100.00,REF,0,200.00\n"
                + "2026-11-05,0.00,100.00,REF,0,200.00\n";
        var a = parser.parse(input.getBytes(StandardCharsets.UTF_8));
        var b = parser.parse(input.getBytes(StandardCharsets.UTF_8));
        assertEquals(2, a.size());
        assertNotEquals(a.get(0).fingerprint(), a.get(1).fingerprint());
        assertEquals(a.get(0).fingerprint(), b.get(0).fingerprint());
    }

    @Test
    void rejectsUnrecognisedAndMalformedStatements() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("a,b,c".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "FOR ACCOUNT NUMBER1234\nDATE,SERVICE FEE,AMOUNT,REFERENCE,CHEQUE NUMBER,BALANCE\n"
                        .concat("2026-11-05,0.00,NOT_A_NUMBER,REF,0,200.00\n")
                        .getBytes(StandardCharsets.UTF_8)));
    }
}
