package za.co.mawa.bes.dto.v2;

import za.co.mawa.bes.enums.payroll.PayrollPaymentItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPaymentItemUpdateRequestDto {

    private String id;
    private String batchId;
    private String employeeId;
    private String employeeNo;
    private String employeeName;
    private String bankName;
    private String branchCode;
    private String accountNo;
    private String accountType;
    private String accountHolderName;
    private Long amountCents;
    private String paymentReference;
    private String salaryReference;
    private PayrollPaymentItemStatus status;
    private Boolean excluded;
    private String exclusionReason;
    private String failureReason;
}
