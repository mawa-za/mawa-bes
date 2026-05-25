package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MembershipPremiumPaymentSyncOfflineRequest {

    private String deviceId;

    private String terminalId;

    private String localPaymentBatchId;

    private String paymentBatchNo;

    private String membershipId;

    private String paymentMethod;

    private Long totalAmountCents;

    private LocalDateTime paymentDate;

    private String location;

    private String employeeResponsible;

    private String createdBy;

    private List<PremiumReceiptOfflineDto> receipts;
}