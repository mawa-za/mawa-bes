package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class PayrollPaymentItemEditRequest {
    private String id; // The unique ID of the item being edited
    private String employeeName;
    private String bankName;
    private String branchCode;
    private String accountNo;
    private Long amountCents;
}