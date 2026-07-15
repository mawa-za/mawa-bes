package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class LeaveRequestStatusHistoryV2Dto {
    private String id;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime changedAt;
    private String changedBy;
}
