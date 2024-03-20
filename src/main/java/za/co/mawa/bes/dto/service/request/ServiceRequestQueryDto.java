package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestQueryDto {
    private String customer;
    private String status;
    private String category;
    private String priority;
    private String dueDate;
    private String creationDate;
}
