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
    void groceryCannotReplaceThePrimaryFuneralArrangementClaimType() {
        InitiateFuneralClaimsDto request = new InitiateFuneralClaimsDto();
        request.setClaimType("GROCERY");

        assertEquals("FUNERAL", request.getEffectiveClaimType(1));
        assertEquals("COMBINATION", request.getEffectiveClaimType(2));
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
