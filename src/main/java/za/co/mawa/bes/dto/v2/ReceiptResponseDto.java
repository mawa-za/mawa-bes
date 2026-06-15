package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.SyncStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponseDto {

    private String id;
    private String receiptNo;
    private String paymentBatchId;
    private String paymentBatchNo;
    private ReceiptSourceType sourceType;
    private String receivedFromPartnerId;
    private String membershipId;
    private LocalDateTime receiptDate;
    private String paymentMethod;
    private Long totalAmountCents;
    private ReceiptStatus status;
    private SyncStatus syncStatus;
    private String location;
    private String employeeResponsible;
    private String deviceId;
    private String terminalId;
    private String externalReceiptNo;
    private Boolean printed;
    private Integer printCount;
    private String legacyPremiumPaymentId;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
