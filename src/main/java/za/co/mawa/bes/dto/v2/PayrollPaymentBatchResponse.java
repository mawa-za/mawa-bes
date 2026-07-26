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

    private String approvalRequestId;
    private String debtorAccountId;
    private String bankMessageStatus;
    private String fnbInstructionId;
    private String bankReportStatus;
    private String bankReportReason;
    private String bankReportJson;
    private LocalDateTime bankQueuedAt;
    private LocalDateTime bankSubmittedAt;
    private LocalDateTime bankReportRetrievedAt;

    private LocalDateTime createdAt;

    private List<PayrollPaymentItemResponse> items;
}