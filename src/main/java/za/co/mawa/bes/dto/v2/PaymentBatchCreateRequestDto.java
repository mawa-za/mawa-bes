package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.SyncStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBatchCreateRequestDto {

    private String paymentBatchNo;
    private ReceiptSourceType sourceType;
    private String receivedFromPartnerId;
    private String membershipId;
    private String paymentMethod;
    private Long totalAmountCents;
    private LocalDateTime paymentDate;
    private String location;
    private String employeeResponsible;
    private String deviceId;
    private String terminalId;
    private String localPaymentBatchId;
    private PaymentBatchStatus status;
    private SyncStatus syncStatus;
    private String notes;
    private String legacyPremiumPaymentId;
}
