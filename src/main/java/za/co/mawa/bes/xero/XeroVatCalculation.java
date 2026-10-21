package za.co.mawa.bes.xero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pure, side-effect-free VAT calculation for prospective Xero invoice snapshots.
 * No tenant configuration lookup or outbound Xero call occurs here.
 *
 * Monetary inputs are amounts for the entire line (after any discounts), not
 * unit prices. Callers must persist the returned snapshot before posting.
 */
public final class XeroVatCalculation {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;

    private XeroVatCalculation() { }

    public enum PricingMethod { INCLUSIVE, EXCLUSIVE }
    public enum TaxTreatment { STANDARD, ZERO_RATED, EXEMPT, NO_TAX }

    public record Result(BigDecimal net, BigDecimal tax, BigDecimal gross,
                         BigDecimal ratePercent, PricingMethod pricingMethod,
                         TaxTreatment taxTreatment) { }

    public static Result calculate(BigDecimal enteredLineAmount, BigDecimal standardRatePercent,
                                   PricingMethod pricingMethod, TaxTreatment taxTreatment,
                                   boolean tenantVatRegistered) {
        Objects.requireNonNull(enteredLineAmount, "enteredLineAmount");
        Objects.requireNonNull(standardRatePercent, "standardRatePercent");
        Objects.requireNonNull(pricingMethod, "pricingMethod");
        Objects.requireNonNull(taxTreatment, "taxTreatment");
        if (standardRatePercent.signum() < 0 || standardRatePercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("VAT rate must be between 0 and 100 percent");
        }
        if (!tenantVatRegistered && taxTreatment == TaxTreatment.STANDARD) {
            throw new IllegalArgumentException("Non-VAT-registered tenant cannot apply standard output VAT");
        }
        BigDecimal rate = taxTreatment == TaxTreatment.STANDARD ? standardRatePercent : BigDecimal.ZERO;
        BigDecimal amount = enteredLineAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal net;
        BigDecimal tax;
        BigDecimal gross;
        if (pricingMethod == PricingMethod.INCLUSIVE && rate.signum() > 0) {
            gross = amount;
            net = gross.multiply(HUNDRED)
                    .divide(HUNDRED.add(rate), MONEY_SCALE, RoundingMode.HALF_UP);
            tax = gross.subtract(net);
        } else {
            net = amount;
            tax = net.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            gross = net.add(tax);
        }
        return new Result(net, tax, gross, rate, pricingMethod, taxTreatment);
    }
}
