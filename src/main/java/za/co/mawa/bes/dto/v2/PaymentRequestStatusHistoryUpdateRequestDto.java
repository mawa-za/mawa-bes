package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.PaymentRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestStatusHistoryUpdateRequestDto {

    private String id;
    private String paymentRequestId;
    private PaymentRequestStatus oldStatus;
    private PaymentRequestStatus newStatus;
    private String comment;
    private LocalDateTime changedAt;
    private String changedBy;
}
