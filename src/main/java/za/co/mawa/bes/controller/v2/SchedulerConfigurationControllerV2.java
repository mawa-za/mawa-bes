package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.schedule.ScheduledJobSettingsDto;
import za.co.mawa.bes.service.v2.SchedulerConfigurationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/scheduler")
public class SchedulerConfigurationControllerV2 {
    private final SchedulerConfigurationService service;

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobs() {
        return ResponseEntity.ok(service.getJobs());
    }

    @GetMapping("/jobs/{jobCode}")
    public ResponseEntity<?> getJob(@PathVariable String jobCode) {
        return ResponseEntity.ok(service.getJob(jobCode));
    }

    @PutMapping("/jobs/{jobCode}")
    public ResponseEntity<?> updateJob(@PathVariable String jobCode, @RequestBody ScheduledJobSettingsDto request) {
        return ResponseEntity.ok(service.updateJob(jobCode, request));
    }

    @PostMapping("/jobs/{jobCode}/run-now")
    public ResponseEntity<?> runNow(@PathVariable String jobCode) {
        return ResponseEntity.ok(service.runNow(jobCode));
    }
}
