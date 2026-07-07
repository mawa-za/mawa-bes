package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PaymentSyncOfflineResponseDto {

    private String syncStatus;

    private String paymentBatchId;

    private String paymentBatchNo;

    private String membershipId;

    private String paidUpToPeriod;

    private List<ReceiptResponseDto> receipts;

    private List<String> warnings;
}