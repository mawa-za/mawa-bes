package za.co.mawa.bes.dto.v2.serviceorder;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ServiceOrderResponse {
    private String id;
    private String serviceOrderNo;
    private String customerPartnerId;
    private String customerName;
    private String assignedEmployeePartnerId;
    private String assignedEmployeeName;
    private String salesAreaId;
    private LocalDate orderDate;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private String status;
    private String location;
    private String notes;
    private Long subtotalCents;
    private Long discountCents;
    private Long taxCents;
    private Long totalCents;
    private String currency;
    private String invoiceId;
    private String invoiceStatus;
    private List<ServiceOrderSourceResponse> sources;
    private List<ServiceOrderLineResponse> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
