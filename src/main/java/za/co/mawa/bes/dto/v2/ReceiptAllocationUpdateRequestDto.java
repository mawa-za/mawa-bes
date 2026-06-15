package za.co.mawa.bes.dto.v2;

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
public class ReceiptAllocationUpdateRequestDto {

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
}
