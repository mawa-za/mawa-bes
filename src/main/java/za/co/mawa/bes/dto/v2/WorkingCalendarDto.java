package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WorkingCalendarDto {
    private String id;
    private String code;
    private String name;
    private String description;
    private Boolean mondayWorking;
    private Boolean tuesdayWorking;
    private Boolean wednesdayWorking;
    private Boolean thursdayWorking;
    private Boolean fridayWorking;
    private Boolean saturdayWorking;
    private Boolean sundayWorking;
    private BigDecimal hoursPerDay;
    private Boolean active;
    private Long version;
    private List<WorkingCalendarHolidayDto> holidays;
}
