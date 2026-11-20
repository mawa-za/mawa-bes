package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

/** Finance user records an independently verified payout recovery or unpaid withdrawal. */
@Getter @Setter
public class FuneralClaimFundingReversalRequestDto {
    private String reason;
    private String reference;
    /** A positive imported credit in the claim-owning tenant. Required for a paid payout. */
    private Long recoveryBankTransactionId;
}
