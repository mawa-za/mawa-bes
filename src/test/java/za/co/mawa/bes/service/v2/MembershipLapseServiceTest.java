package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipLapseServiceTest {

    @Test
    void countsConsecutiveOutstandingPremiums() {
        assertEquals(3, MembershipLapseService.countConsecutiveMissedPremiums(List.of(
                outstanding(2026, 6),
                outstanding(2026, 5),
                outstanding(2026, 4)
        )));
    }

    @Test
    void paidPremiumBreaksTheMissedSequence() {
        assertEquals(2, MembershipLapseService.countConsecutiveMissedPremiums(List.of(
                outstanding(2026, 6),
                outstanding(2026, 5),
                settled(2026, 4),
                outstanding(2026, 3)
        )));
    }

    @Test
    void missingCalendarPeriodBreaksTheMissedSequence() {
        assertEquals(1, MembershipLapseService.countConsecutiveMissedPremiums(List.of(
                outstanding(2026, 6),
                outstanding(2026, 4),
                outstanding(2026, 3)
        )));
    }

    @Test
    void settledLatestPremiumMeansThereIsNoCurrentMissedSequence() {
        assertEquals(0, MembershipLapseService.countConsecutiveMissedPremiums(List.of(
                settled(2026, 6),
                outstanding(2026, 5),
                outstanding(2026, 4)
        )));
    }

    @Test
    void legacyTransactionUpdateUsesPhysicalSnakeCaseColumn() {
        String sql = MembershipLapseService.LEGACY_TRANSACTION_LAPSE_SQL;

        assertTrue(sql.contains("changed_by = ?"));
        assertFalse(sql.contains("changedBy"));
    }

    private MembershipLapseService.PremiumPeriod outstanding(int year, int month) {
        return new MembershipLapseService.PremiumPeriod(
                YearMonth.of(year, month),
                true,
                false
        );
    }

    private MembershipLapseService.PremiumPeriod settled(int year, int month) {
        return new MembershipLapseService.PremiumPeriod(
                YearMonth.of(year, month),
                false,
                true
        );
    }
}
