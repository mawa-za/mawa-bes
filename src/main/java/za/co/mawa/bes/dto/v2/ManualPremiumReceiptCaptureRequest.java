package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ManualPremiumReceiptCaptureRequest {
    private String membershipId;
    private Long amountCents;
    private String paymentMethod;
    private LocalDate originalReceiptDate;
    private String receiptBookNo;
    private String manualReceiptNo;
    private String originalCollectorEmployeeId;
    private String locationAreaCode;
    private String captureMode; // LEGACY_CATCH_UP or MANUAL_EMERGENCY
    private String lateCaptureReason;
    private String proofAttachmentId;
    private String createdBy;
    private String notes;
}
