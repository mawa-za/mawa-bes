package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.TrustPaymentMethod;
import za.co.mawa.bes.enums.TrustTransactionDirection;
import za.co.mawa.bes.enums.TrustTransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTrustTransactionResponseDto {

    private String id;
    private String caseId;
    private String clientPartnerId;
    private String transactionNo;
    private TrustTransactionType transactionType;
    private TrustTransactionDirection direction;
    private Long amountCents;
    private Long balanceAfterCents;
    private TrustPaymentMethod paymentMethod;
    private String referenceNo;
    private String bankReference;
    private String payeeName;
    private String description;
    private String relatedInvoiceId;
    private String relatedReceiptId;
    private String relatedTransactionId;
    private LocalDateTime transactionDate;
    private Boolean reversed;
    private LocalDateTime reversedAt;
    private String reversedBy;
    private String reversalReason;
    private LocalDateTime createdAt;
    private String createdBy;
}
