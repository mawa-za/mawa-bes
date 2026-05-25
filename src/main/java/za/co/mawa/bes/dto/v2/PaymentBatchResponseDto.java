package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class PaymentBatchResponseDto {

    private String id;

    private String paymentBatchNo;

    private String sourceType;

    private String membershipId;

    private String paymentMethod;

    private Long totalAmountCents;

    private LocalDateTime paymentDate;

    private String status;

    private String syncStatus;

    private String paidUpToPeriod;

    private List<ReceiptResponseDto> receipts;
}
