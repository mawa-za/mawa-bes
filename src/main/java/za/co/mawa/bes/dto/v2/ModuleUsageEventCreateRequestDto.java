package za.co.mawa.bes.dto.v2;

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
public class ModuleUsageEventCreateRequestDto {

    private String userId;
    private String moduleCode;
    private String moduleName;
    private String modulePath;
    private String workcenterId;
    private LocalDateTime usedAt;
}
