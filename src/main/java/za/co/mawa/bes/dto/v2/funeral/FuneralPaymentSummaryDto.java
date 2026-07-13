package za.co.mawa.bes.dto.v2.funeral;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralPaymentSummaryDto {
    private String funeralServiceInvoiceId;
    private String funeralServiceId;
    private String serviceRequestNo;
    private String deceasedName;
    private String invoiceId;
    private String invoiceNo;
    private String entityType;
    private String partnerId;
    private Long allocatedAmountCents;
    private Long invoiceTotalCents;
    private Long paidCents;
    private Long balanceCents;
    private String status;
    private LocalDate invoiceDate;
}
