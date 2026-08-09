package za.co.mawa.bes.dto.v2.appointment.serviceorder;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AppointmentServiceOrderRequest {
    private LocalDate serviceDate;
    private String assignedEmployeePartnerId;
    private String status;
    private String location;
    private String notes;
    private List<ServiceOrderLineRequest> lines;
}
