package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestEditDto implements Serializable {
    private String id;
    private String statusReason;
    private String description;
    private String category;
    private String priority;
    private String assignee;
    private String status;
}
