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
    private LocalDateTime createdAt;
    private String createdBy;
}
