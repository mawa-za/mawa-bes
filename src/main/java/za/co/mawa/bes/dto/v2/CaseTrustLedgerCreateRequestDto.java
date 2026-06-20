package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTrustLedgerCreateRequestDto {

    private String caseId;
    private String clientPartnerId;
    private String currency;
    private Long totalReceivedCents;
    private Long totalTransferredCents;
    private Long totalRefundedCents;
    private Long totalPaidOutCents;
    private Long availableBalanceCents;
}
