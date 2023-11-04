package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestQueryResultDto {
    private String id;
    private String no;
    private String customerId;
    private String customerName;
    private String employeeResponsibleId;
    private String employeeResponsibleName;
    private String summary;
    private String description;
    private String category;
    private String priority;
    private String status;
}
