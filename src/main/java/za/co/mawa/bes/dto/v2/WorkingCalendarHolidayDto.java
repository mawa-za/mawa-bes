package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.time.LocalDate;

@Data
public class WorkingCalendarHolidayDto {
    private String id;
    private LocalDate holidayDate;
    private String name;
    private Boolean recurringAnnual;
    private Boolean active;
}
