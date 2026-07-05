package za.co.mawa.bes.dto.v2.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageQueueAdminDto {
    private Long id;
    private String type;
    private String referenceId;
    private String referenceNo;
    private String payload;
    private boolean processed;
    private String status;
    private int retryCount;
    private LocalDateTime nextAttemptAt;
}
