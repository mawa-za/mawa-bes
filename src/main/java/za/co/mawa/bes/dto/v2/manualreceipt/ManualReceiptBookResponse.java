package za.co.mawa.bes.dto.v2.manualreceipt;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ManualReceiptBookResponse {
    private String id;
    private String receiptBookNo;
    private String description;
    private String receiptFromNo;
    private String receiptToNo;
    private String assignedEmployeeId;
    private String assignedEmployeeName;
    private String assignedAreaCode;
    private String assignedAreaName;
    private String status;
    private Boolean active;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
