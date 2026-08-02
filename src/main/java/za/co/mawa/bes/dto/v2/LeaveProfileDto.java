package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveProfileDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private String workingCalendarId;
    private String workingCalendarName;
    private Boolean defaultProfile;
    private LocalDate activeFrom;
    private LocalDate activeTo;
    private Boolean active;
    private Long version;
    private List<LeaveProfileRuleDto> rules;
}
