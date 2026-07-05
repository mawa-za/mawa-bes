package za.co.mawa.bes.dto.v2.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueScheduleSettingsDto {
    private boolean enabled;
    private int intervalSeconds;
    private int batchSize;
    private String lastRunAt;
    private String nextRunAt;
}
