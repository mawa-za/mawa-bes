package za.co.mawa.bes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueResponseDto {

    private Long id;
    private String type;
    private String referenceId;
    private String referenceNo;
    private String payload;
    private boolean processed;
    private int retryCount;
    private LocalDateTime nextAttemptAt;
}
