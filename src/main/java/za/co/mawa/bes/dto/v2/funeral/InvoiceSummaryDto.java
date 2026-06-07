package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceSummaryDto {
    private String invoiceId;
    private String invoiceNo;
    private String status;
    private Long totalCents;
    private Long paidCents;
    private Long balanceCents;
}
