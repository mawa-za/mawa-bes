package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralServiceInvoiceUpdateRequestDto {

    private String id;
    private String funeralServiceId;
    private String invoiceId;
    private String entityType;
    private String partnerId;
    private String membershipClaimId;
    private Long amountCents;
}
