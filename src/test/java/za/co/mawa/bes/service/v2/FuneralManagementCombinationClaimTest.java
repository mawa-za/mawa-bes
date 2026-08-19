package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuneralManagementCombinationClaimTest {

    @Test
    void combinationCoverKeepsItsFullBenefitEvenWhenArrangementRemainingIsZero() {
        long amount = FuneralManagementService.claimAmountForSelectedCover(
                "COMBINATION",
                12_500L,
                10_000L,
                0L);

        assertEquals(12_500L, amount);
    }

    @Test
    void combinationCoverIsNotCappedByFuneralArrangementTotal() {
        long amount = FuneralManagementService.claimAmountForSelectedCover(
                "COMBINATION",
                20_000L,
                15_000L,
                15_000L);

        assertEquals(20_000L, amount);
    }

    @Test
    void singleFuneralCoverRemainsCappedToOutstandingArrangementAmount() {
        long amount = FuneralManagementService.claimAmountForSelectedCover(
                "FUNERAL",
                20_000L,
                15_000L,
                4_500L);

        assertEquals(4_500L, amount);
    }

    @Test
    void claimWithoutArrangementTotalUsesFullConfiguredBenefit() {
        long amount = FuneralManagementService.claimAmountForSelectedCover(
                "FUNERAL",
                8_000L,
                0L,
                Long.MAX_VALUE);

        assertEquals(8_000L, amount);
    }
}
