package za.co.mawa.bes.dto.v2.serviceorder;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceOrderRequest {
    private String customerPartnerId;
    @JsonAlias("serviceDate")
    private LocalDate orderDate;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private String assignedEmployeePartnerId;
    private String salesAreaId;
    private String status;
    private String location;
    private String notes;
    private String currency;
    private List<ServiceOrderLineRequest> lines;
}
