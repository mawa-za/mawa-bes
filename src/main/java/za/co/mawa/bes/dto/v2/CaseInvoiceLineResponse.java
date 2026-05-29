package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseInvoiceLineResponse {
    private String sourceType;
    private String sourceId;
    private String description;
    private Long quantityMinutes;
    private Long unitPriceCents;
    private Long amountCents;
}
