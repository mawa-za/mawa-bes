package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestCreateDto implements Serializable {
    private String customerId;
    private String summary;
    private String description;
    private String category;
    private String priority;
}
