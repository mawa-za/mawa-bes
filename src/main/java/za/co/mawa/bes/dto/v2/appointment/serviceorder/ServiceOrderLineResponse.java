package za.co.mawa.bes.dto.v2.appointment.serviceorder;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceOrderLineResponse {
    private String id;
    private String productId;
    private String description;
    private Double quantity;
    private Long unitPriceCents;
    private Long discountCents;
    private Long taxCents;
    private Long subtotalCents;
    private Long totalCents;
    private String employeePartnerId;
    private Integer sortOrder;
}
