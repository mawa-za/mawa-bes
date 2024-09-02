package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestCreateDto implements Serializable {
    private String customer;
    private String description;
    private String category;
    private String priority;
    private List<String> assignees;
    private String summary;
}
