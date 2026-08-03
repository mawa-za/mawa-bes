package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataValidationServiceTest {

    private final ReferenceDataValidationService service =
            new ReferenceDataValidationService(null);

    @Test
    void contactNumberNormalizesCommonSouthAfricanFormats() {
        assertEquals("0821234567", service.requireContactNumber("+27 82 123 4567"));
        assertEquals("0821234567", service.requireContactNumber("27-82-123-4567"));
        assertEquals("0821234567", service.requireContactNumber("082 123 4567"));
    }

    @Test
    void contactNumberRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> service.requireContactNumber(""));
        assertThrows(IllegalArgumentException.class, () -> service.requireContactNumber("821234567"));
        assertThrows(IllegalArgumentException.class, () -> service.requireContactNumber("01234"));
    }
}
