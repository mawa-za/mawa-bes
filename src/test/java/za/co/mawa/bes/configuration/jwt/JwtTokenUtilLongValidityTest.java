package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenUtilLongValidityTest {
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long ONE_YEAR_MS = 365L * 24 * 60 * 60 * 1000;

    @Test void newlyIssuedTokensUseThirtyDaysAndThreeHundredSixtyFiveDays() {
        var util = new JwtTokenUtil();
        util.setSecret("unit-test-only-signing-key-at-least-sixty-four-bytes-long-for-HS512-validation");
        util.setJwtExpirationInMs(THIRTY_DAYS_MS);
        util.setRefreshExpirationDateInMs(ONE_YEAR_MS);
        util.init();

        Claims access = util.getClaimFromToken(util.generateToken("cashier", "tenant-a"), claims -> claims);
        Claims refresh = util.getClaimFromToken(util.generateRefreshToken("cashier", "tenant-a"), claims -> claims);

        assertEquals("access", access.get("token_type"));
        assertEquals("refresh", refresh.get("token_type"));
        assertEquals("tenant-a", access.getAudience());
        assertEquals("tenant-a", refresh.getAudience());
        assertTrue(Math.abs(access.getExpiration().getTime() - access.getIssuedAt().getTime() - THIRTY_DAYS_MS) < 1000);
        assertTrue(Math.abs(refresh.getExpiration().getTime() - refresh.getIssuedAt().getTime() - ONE_YEAR_MS) < 1000);
    }
}
