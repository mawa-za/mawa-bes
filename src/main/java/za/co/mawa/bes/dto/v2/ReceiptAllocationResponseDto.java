package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptAllocationResponseDto {

    private String id;
    private String receiptId;
    private ReceiptAllocationType allocationType;
    private String referenceId;
    private String referenceNo;
    private String periodYYYYMM;
    private String membershipId;
    private Long amountCents;
    private ReceiptStatus status;
    private String legacyPremiumPaymentId;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
