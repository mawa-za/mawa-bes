package za.co.mawa.bes.dto.v2.appointment;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AppointmentResponse {
    private String id;
    private String appointmentNo;
    private String number;
    private String status;
    private LocalDate appointmentDate;
    private String bookDate;
    private LocalTime startTime;
    private String bookTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String location;
    private String notes;
    private String sourceType;
    private String sourceId;
    private String customerPartnerId;
    private String employeePartnerId;
    private String responsibleUserId;
    private String serviceProductId;
    private String serviceLocationId;
    private PartnerDto customer;
    private PartnerDto customerPartner;
    private PartnerDto employeeResponsible;
    private PartnerDto employee;
    private ProductDto product;
    private ProductDto productDto;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
