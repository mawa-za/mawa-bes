package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PayrollPaymentBatchResponse {

    private String id;

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

    private LocalDateTime createdAt;

    private List<PayrollPaymentItemResponse> items;
}