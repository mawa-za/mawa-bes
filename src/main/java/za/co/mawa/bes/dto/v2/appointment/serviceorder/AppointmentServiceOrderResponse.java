package za.co.mawa.bes.dto.v2.appointment.serviceorder;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AppointmentServiceOrderResponse {
    private String id;
    private String serviceOrderNo;
    private String appointmentId;
    private String appointmentNo;
    private String customerPartnerId;
    private String customerName;
    private String assignedEmployeePartnerId;
    private String assignedEmployeeName;
    private LocalDate serviceDate;
    private String status;
    private String location;
    private String notes;
    private Long subtotalCents;
    private Long discountCents;
    private Long taxCents;
    private Long totalCents;
    private String invoiceId;
    private List<ServiceOrderLineResponse> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
