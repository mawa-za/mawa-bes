package za.co.mawa.bes.dto.v2.funeral;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.enums.MembershipClaimType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitiateFuneralClaimsDtoTest {

    @Test
    void groceryIsAPlanBenefitClaimType() {
        assertEquals(MembershipClaimType.GROCERY, MembershipClaimType.valueOf("GROCERY"));
    }

    @Test
    void primaryFuneralClaimTypeIsDerivedFromSelectedCoverCount() {
        InitiateFuneralClaimsDto request = new InitiateFuneralClaimsDto();
        request.setClaimType("GROCERY");

        assertEquals("FUNERAL", request.getEffectiveClaimType(1));
        assertEquals("COMBINATION", request.getEffectiveClaimType(2));
        assertEquals("COMBINATION", request.getEffectiveClaimType(3));
    }

    @Test
    void olderClientsCannotForceCombinationForASingleCoverOrFuneralForMultipleCovers() {
        InitiateFuneralClaimsDto request = new InitiateFuneralClaimsDto();

        request.setClaimType("COMBINATION");
        assertEquals("FUNERAL", request.getEffectiveClaimType(1));

        request.setClaimType("FUNERAL");
        assertEquals("COMBINATION", request.getEffectiveClaimType(2));
    }

    @Test
    void invoicePreviewUsesCombinationForMultipleSelectedCovers() {
        FuneralInvoicePreviewRequestDto request = new FuneralInvoicePreviewRequestDto();

        assertEquals("FUNERAL", request.getEffectiveClaimType(1));
        assertEquals("COMBINATION", request.getEffectiveClaimType(2));
        assertEquals("COMBINATION", request.getEffectiveClaimType(3));
    }

    @Test
    void combinationUsesTheConfiguredCombinationBenefitWithoutFuneralFallback() {
        FuneralMembershipCoverDto noCombinationBenefit = FuneralMembershipCoverDto.builder()
                .funeralAmountCents(100_000L)
                .combinationAmountCents(null)
                .build();
        FuneralMembershipCoverDto combinationBenefit = FuneralMembershipCoverDto.builder()
                .funeralAmountCents(100_000L)
                .combinationAmountCents(75_000L)
                .build();

        assertEquals(0L, noCombinationBenefit.amountForClaimType("COMBINATION"));
        assertEquals(75_000L, combinationBenefit.amountForClaimType("COMBINATION"));
    }

    @Test
    void groceryEligibilityRequiresAPositiveConfiguredBenefit() {
        FuneralMembershipCoverDto noBenefit = FuneralMembershipCoverDto.builder()
                .groceryAmountCents(0L)
                .build();
        FuneralMembershipCoverDto configuredBenefit = FuneralMembershipCoverDto.builder()
                .groceryAmountCents(25_000L)
                .build();

        assertFalse(noBenefit.hasGroceryBenefit());
        assertTrue(configuredBenefit.hasGroceryBenefit());
    }
}
