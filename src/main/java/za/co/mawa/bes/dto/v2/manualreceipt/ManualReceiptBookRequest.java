package za.co.mawa.bes.dto.v2.manualreceipt;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ManualReceiptBookRequest {
    private String receiptBookNo;
    private String description;
    private String receiptFromNo;
    private String receiptToNo;
    private String assignedEmployeeId;
    private String assignedAreaCode;
    private String status;
    private Boolean active;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
}
