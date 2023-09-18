package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestDto {
    private String customerId;
    private String summary;
    private String description;
    private String category;
    private String priority;
}
