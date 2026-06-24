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
public class PayrollPaymentBatchAuditResponseDto {

    private String id;
    private String batchId;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String message;
    private LocalDateTime createdAt;
    private String createdBy;
}
