package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PremiumGenerationServiceTest {

    @Test
    void normalisesLegacyFirstDayMode() {
        assertEquals(
                PremiumGenerationService.DAY_OF_MONTH,
                PremiumGenerationService.normalizeMode("FIRST_DAY_OF_MONTH")
        );
        assertEquals(
                PremiumGenerationService.MONTH_AFTER_LAST_PAYMENT,
                PremiumGenerationService.normalizeMode("month_after_last_payment")
        );
    }

    @Test
    void clampsGenerationDayToLastDayOfShortMonth() {
        assertEquals(28, PremiumGenerationService.effectiveGenerationDay(YearMonth.of(2026, 2), 31));
        assertEquals(29, PremiumGenerationService.effectiveGenerationDay(YearMonth.of(2028, 2), 31));
        assertEquals(30, PremiumGenerationService.effectiveGenerationDay(YearMonth.of(2026, 4), 31));
        assertEquals(15, PremiumGenerationService.effectiveGenerationDay(YearMonth.of(2026, 7), 15));
    }

    @Test
    void returnsExactlySixConsecutivePeriodsEndingAtRequestedPeriod() {
        assertEquals(
                List.of(
                        YearMonth.of(2026, 2),
                        YearMonth.of(2026, 3),
                        YearMonth.of(2026, 4),
                        YearMonth.of(2026, 5),
                        YearMonth.of(2026, 6),
                        YearMonth.of(2026, 7)
                ),
                PremiumGenerationService.sixPeriodsEnding(YearMonth.of(2026, 7))
        );
    }
}
