package za.co.mawa.bes.dto.v2.schedule;

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
public class ScheduledJobSettingsDto {
    private String jobCode;
    private String name;
    private String description;
    private boolean enabled;
    private int intervalMinutes;
    private String lastRunAt;
    private String nextRunAt;
}
