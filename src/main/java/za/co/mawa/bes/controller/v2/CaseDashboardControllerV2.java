package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseDashboardResponse;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.service.v2.CaseDashboardService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseDashboardControllerV2 {

    private final CaseDashboardService caseDashboardService;

    @GetMapping("/dashboard")
    public CaseDashboardResponse dashboard() {
        return caseDashboardService.getDashboard();
    }

    @GetMapping("/tasks/overdue")
    public List<CaseTaskEntity> overdueTasks() {
        return caseDashboardService.getOverdueTasks();
    }

    @GetMapping("/events/upcoming")
    public List<CaseEventEntity> upcomingEvents(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        LocalDateTime start = from != null ? from : LocalDateTime.now();
        LocalDateTime end = to != null ? to : start.plusDays(14);
        return caseDashboardService.getUpcomingEvents(start, end);
    }

    @GetMapping("/billing/unbilled/time-entries")
    public List<CaseTimeEntryEntity> unbilledTimeEntries(
            @RequestParam(required = false) String caseId
    ) {
        return caseDashboardService.getUnbilledTimeEntries(caseId);
    }

    @GetMapping("/billing/unbilled/disbursements")
    public List<CaseDisbursementEntity> unbilledDisbursements(
            @RequestParam(required = false) String caseId
    ) {
        return caseDashboardService.getUnbilledDisbursements(caseId);
    }
}
