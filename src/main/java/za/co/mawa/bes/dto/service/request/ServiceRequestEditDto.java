package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestEditDto implements Serializable {
    private String id;
    private String statusReason;
    private String description;
    private String category;
    private String priority;
    private List<String> assigneeIds;
    private String status;


}

