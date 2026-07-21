package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyNumberRangeConfigurationRequestDto {
    private String object;
    private String prefix;
    private String start;
    private String current;
    private String end;
    private LocalDate validFrom;
    private LocalDate validTo;
}
