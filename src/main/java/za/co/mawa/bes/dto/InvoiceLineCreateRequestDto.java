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

    private String invoice;
    private String productIdId;
    private String descriptionId;
    private String quantityId;
    private String unitPriceCentsId;
    private Long discountCents;
    private Long taxCents;
    private Long subtotalCents;
    private Long totalCents;
}
