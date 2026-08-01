package za.co.mawa.bes.dto.v2.serviceorder;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceOrderLineRequest {
    private String productId;
    private String itemType;
    private String description;
    private Double quantity;
    private Long unitPriceCents;
    private Long discountCents;
    private Long taxCents;
    private String employeePartnerId;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private String completionStatus;
}
