package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentBatchAuditUpdateRequestDto {

    private String id;
    private String batchId;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String message;
}
