package za.co.mawa.bes.dto.v2.serviceorder;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ServiceOrderLineResponse {
    private String id;
    private String productId;
    private String itemType;
    private String description;
    private Double quantity;
    private Long unitPriceCents;
    private Long discountCents;
    private Long taxCents;
    private Long subtotalCents;
    private Long totalCents;
    private String employeePartnerId;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private String completionStatus;
    private Integer sortOrder;
}
