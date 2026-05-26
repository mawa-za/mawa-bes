package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReceiptAllocationResponseDto {

    private String id;

    private String allocationType;

    private String referenceId;

    private String referenceNo;

    private String periodYYYYMM;

    private String membershipId;

    private Long amountCents;

    private String status;
}