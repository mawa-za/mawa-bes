package za.co.mawa.bes.dto.v2.appointment.serviceorder;

import lombok.Data;

@Data
public class ServiceOrderLineRequest {
    private String id;
    private String productId;
    private String description;
    private Double quantity;
    private Long unitPriceCents;
    private Long discountCents;
    private Long taxCents;
    private String employeePartnerId;
}
