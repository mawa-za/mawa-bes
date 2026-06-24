package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineCreateRequestDto {
    private String invoiceId;
    private String productId;
    private String description;
    private Double quantity;
    private Long unitPriceCents;
    private Long discountCents;
    private Long taxCents;
    private Long subtotalCents;
    private Long totalCents;
}
