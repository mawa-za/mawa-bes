package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseTrustBalanceResponse {

    private String caseId;
    private String clientPartnerId;
    private String currency;

    private Long totalReceivedCents;
    private Long totalTransferredCents;
    private Long totalRefundedCents;
    private Long totalPaidOutCents;
    private Long availableBalanceCents;
}
