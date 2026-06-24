package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTrustLedgerResponseDto {

    private String id;
    private String caseId;
    private String clientPartnerId;
    private String currency;
    private Long totalReceivedCents;
    private Long totalTransferredCents;
    private Long totalRefundedCents;
    private Long totalPaidOutCents;
    private Long availableBalanceCents;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
