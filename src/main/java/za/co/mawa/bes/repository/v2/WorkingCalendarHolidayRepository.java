package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.WorkingCalendarHolidayEntity;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface WorkingCalendarHolidayRepository extends JpaRepository<WorkingCalendarHolidayEntity, String> {
    List<WorkingCalendarHolidayEntity> findByWorkingCalendarIdOrderByHolidayDateAsc(String workingCalendarId);
    List<WorkingCalendarHolidayEntity> findByWorkingCalendarIdAndActiveTrueAndHolidayDateBetween(String workingCalendarId, LocalDate from, LocalDate to);
    List<WorkingCalendarHolidayEntity> findByWorkingCalendarIdAndActiveTrueAndRecurringAnnualTrue(String workingCalendarId);
}
