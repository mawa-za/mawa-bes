package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentBatchCreateRequestDto {

    private String batchNo;
    private String description;
    private String payPeriod;
    private LocalDate paymentDate;
    private String sourceBatchId;
    private PayrollPaymentBatchStatus status;
    private Integer totalEmployees;
    private Long totalAmountCents;
    private Boolean eftFileGenerated;
    private String eftFileName;
    private LocalDateTime eftFileGeneratedAt;
    private String notes;
}
