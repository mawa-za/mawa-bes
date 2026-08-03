package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReceiptVerificationDto {
    private String traceId;
    private String receiptNo;
    private LocalDateTime receiptDate;
    private Long amountCents;
    private String status;
    private boolean authentic;
}
