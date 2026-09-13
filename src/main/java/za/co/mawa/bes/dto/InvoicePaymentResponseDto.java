package za.co.mawa.bes.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentResponseDto {
    private String id;
    private String invoiceId;
    private LocalDateTime paymentDate;
    private Long amountCents;
    private String paymentMethod;
    private String referenceNo;
    private String receiptId;
    private String status;
    private LocalDateTime reversedAt;
    private String reversedBy;
    private String reversalReason;
    private String xeroPaymentId;
    private String xeroSyncStatus;
    private String xeroSyncError;
    private LocalDateTime xeroSyncedAt;
    private LocalDateTime createdAt;
    private String createdBy;
}
