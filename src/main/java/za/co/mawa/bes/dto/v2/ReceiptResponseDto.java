package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReceiptResponseDto {

    private String id;

    private String receiptNo;

    private String paymentBatchId;

    private String paymentBatchNo;

    private String sourceType;

    private String membershipId;

    private LocalDateTime receiptDate;

    private String paymentMethod;

    private Long totalAmountCents;

    private String status;

    private String syncStatus;

    private Boolean printed;

    private Integer printCount;

    private List<ReceiptAllocationResponseDto> allocations;
}