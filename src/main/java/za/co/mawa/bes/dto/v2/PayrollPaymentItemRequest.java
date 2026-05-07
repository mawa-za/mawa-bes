package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayrollPaymentItemRequest {

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
}
