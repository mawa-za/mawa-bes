package za.co.mawa.bes.dto.v2.group;

import lombok.Data;
import java.util.List;

@Data
public class GroupSocietyStatusChangeRequest {
    private String requestedBy;
    private String notes;
    private List<String> supportingAttachmentIds;
}
