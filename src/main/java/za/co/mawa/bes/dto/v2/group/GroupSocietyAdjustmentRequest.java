package za.co.mawa.bes.dto.v2.group;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class GroupSocietyAdjustmentRequest {
    private Long amountCents;
    private LocalDate adjustmentDate;
    private String direction;
    private String referenceNo;
    private String notes;
    private String requestedBy;
    private List<String> supportingAttachmentIds;
}
