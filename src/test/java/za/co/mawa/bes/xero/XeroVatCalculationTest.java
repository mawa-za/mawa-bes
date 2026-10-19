package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static za.co.mawa.bes.xero.XeroVatCalculation.*;

class XeroVatCalculationTest {
    private static final BigDecimal VAT = new BigDecimal("15");

    @Test void inclusiveExtractsVat() {
        Result result = calculate(new BigDecimal("115.00"), VAT, PricingMethod.INCLUSIVE,
                TaxTreatment.STANDARD, true);
        assertEquals(new BigDecimal("100.00"), result.net());
        assertEquals(new BigDecimal("15.00"), result.tax());
        assertEquals(new BigDecimal("115.00"), result.gross());
    }

    @Test void exclusiveAddsVat() {
        Result result = calculate(new BigDecimal("100.00"), VAT, PricingMethod.EXCLUSIVE,
                TaxTreatment.STANDARD, true);
        assertEquals(new BigDecimal("100.00"), result.net());
        assertEquals(new BigDecimal("15.00"), result.tax());
        assertEquals(new BigDecimal("115.00"), result.gross());
    }

    @Test void zeroRatedAndExemptDoNotChargeVat() {
        for (TaxTreatment treatment : new TaxTreatment[]{TaxTreatment.ZERO_RATED, TaxTreatment.EXEMPT}) {
            Result result = calculate(new BigDecimal("120.00"), VAT, PricingMethod.INCLUSIVE, treatment, true);
            assertEquals(new BigDecimal("0.00"), result.tax());
            assertEquals(new BigDecimal("120.00"), result.gross());
        }
    }

    @Test void unregisteredTenantDoesNotChargeVat() {
        Result result = calculate(new BigDecimal("100.00"), VAT, PricingMethod.EXCLUSIVE,
                TaxTreatment.NO_TAX, false);
        assertEquals(new BigDecimal("0.00"), result.tax());
        assertThrows(IllegalArgumentException.class,
                () -> calculate(new BigDecimal("100"), VAT, PricingMethod.EXCLUSIVE,
                        TaxTreatment.STANDARD, false));
    }

    @Test void roundingIsExplicit() {
        Result result = calculate(new BigDecimal("0.05"), VAT, PricingMethod.INCLUSIVE,
                TaxTreatment.STANDARD, true);
        assertEquals(new BigDecimal("0.04"), result.net());
        assertEquals(new BigDecimal("0.01"), result.tax());
        assertEquals(new BigDecimal("0.05"), result.gross());
    }

    @Test void negativeCreditAmountsPreserveSign() {
        Result result = calculate(new BigDecimal("-115.00"), VAT, PricingMethod.INCLUSIVE,
                TaxTreatment.STANDARD, true);
        assertEquals(new BigDecimal("-100.00"), result.net());
        assertEquals(new BigDecimal("-15.00"), result.tax());
    }

    @Test void rejectsInvalidVatRate() {
        assertThrows(IllegalArgumentException.class,
                () -> calculate(BigDecimal.ONE, new BigDecimal("-1"), PricingMethod.EXCLUSIVE,
                        TaxTreatment.STANDARD, true));
    }
}
