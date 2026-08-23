package za.co.mawa.bes.dto.v2.appointment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@Getter
@Setter
public class AppointmentRequest {
    private String customerId;
    private String customerPartnerId;
    private String employeeId;
    private String employeePartnerId;
    private String responsibleUserId;
    private String productId;
    private String serviceProductId;
    private String serviceLocationId;
    private LocalDate appointmentDate;
    private String bookDate;
    private LocalTime startTime;
    private String bookTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String status;
    private String location;
    private String notes;
    private String sourceType;
    private String sourceId;
}
