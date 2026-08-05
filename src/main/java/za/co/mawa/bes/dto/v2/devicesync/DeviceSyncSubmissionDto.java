package za.co.mawa.bes.dto.v2.devicesync;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;

@Value
@Builder
public class DeviceSyncSubmissionDto {
    String submissionId;
    String idempotencyKey;
    String deviceId;
    String submittedBy;
    String method;
    String path;
    Object requestPayload;
    Object responsePayload;
    Integer responseStatus;
    String status;
    int attemptCount;
    String errorMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime processedAt;
}
