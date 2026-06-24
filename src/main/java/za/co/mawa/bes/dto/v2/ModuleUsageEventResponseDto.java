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
public class ModuleUsageEventResponseDto {

    private String id;
    private String userId;
    private String moduleCode;
    private String moduleName;
    private String modulePath;
    private String workcenterId;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private String createdBy;
}
