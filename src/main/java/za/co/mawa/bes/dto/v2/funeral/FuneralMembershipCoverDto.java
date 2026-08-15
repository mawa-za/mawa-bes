package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FuneralMembershipCoverDto {
    /**
     * Stable selection id used by funeral screens.
     * LOCAL_TENANT values are formatted as LOCAL:{membershipId}:{deceasedPartnerId}:{deceasedType}.
     * Live EXTERNAL_TENANT values are formatted as EXTERNAL:{tenantId}:{membershipId}:{deceasedPartnerId}:{deceasedType}. Legacy external-cover snapshots remain supported as EXTERNAL:{externalCoverId}.
     */
    private String membershipId;
    private String membershipNumber;
    private String burialSocietyName;
    private String burialSocietyPartnerId;

    /**
     * Backwards compatible amount. This remains the normal FUNERAL amount.
     */
    private Long coverAmountCents;

    /** Amount payable when this cover is used as a normal FUNERAL claim. */
    private Long funeralAmountCents;

    /** Amount payable when more than one cover is selected and claims are handled as COMBINATION. */
    private Long combinationAmountCents;

    /** Optional grocery benefit configured against the membership plan. */
    private Long groceryAmountCents;

    private String coverSource;
    private String sourceTenantId;
    private String sourceTenantName;
    private String sourceMembershipId;
    private String sourceReference;
    private String deceasedPartnerId;
    private String deceasedType;

    public boolean hasGroceryBenefit() {
        return groceryAmountCents != null && groceryAmountCents > 0L;
    }

    public Long amountForClaimType(String claimType) {
        if ("COMBINATION".equalsIgnoreCase(claimType)) {
            return combinationAmountCents == null ? 0L : Math.max(0L, combinationAmountCents);
        }
        return firstPositive(funeralAmountCents, coverAmountCents, combinationAmountCents);
    }

    private Long firstPositive(Long... values) {
        if (values == null) return 0L;
        for (Long value : values) {
            if (value != null && value > 0) return value;
        }
        return 0L;
    }
}
