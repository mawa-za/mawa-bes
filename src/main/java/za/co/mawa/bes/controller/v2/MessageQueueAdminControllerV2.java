package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.message.MessageQueueAdminDto;
import za.co.mawa.bes.dto.v2.message.MessageQueueAdminListResponse;
import za.co.mawa.bes.dto.v2.message.MessageQueueScheduleSettingsDto;
import za.co.mawa.bes.service.MessageQueueAdminService;

import java.util.Map;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2/message-queue")
public class MessageQueueAdminControllerV2 {
    private final MessageQueueAdminService service;

    @GetMapping
    public ResponseEntity<MessageQueueAdminListResponse> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(service.search(type, status, reference, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageQueueAdminDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<MessageQueueAdminDto> retry(@PathVariable Long id) {
        return ResponseEntity.ok(service.retry(id));
    }

    @PostMapping("/{id}/mark-processed")
    public ResponseEntity<MessageQueueAdminDto> markProcessed(@PathVariable Long id) {
        return ResponseEntity.ok(service.markProcessed(id));
    }

    @PostMapping("/process-now")
    public ResponseEntity<Map<String, Object>> processNow() {
        return ResponseEntity.ok(Map.of("processed", service.processNow()));
    }

    @GetMapping("/schedule")
    public ResponseEntity<MessageQueueScheduleSettingsDto> getSchedule() {
        return ResponseEntity.ok(service.getScheduleSettings());
    }

    @PutMapping("/schedule")
    public ResponseEntity<MessageQueueScheduleSettingsDto> updateSchedule(@RequestBody MessageQueueScheduleSettingsDto request) {
        return ResponseEntity.ok(service.updateScheduleSettings(request));
    }

    @PostMapping("/schedule/start")
    public ResponseEntity<MessageQueueScheduleSettingsDto> startSchedule() {
        return ResponseEntity.ok(service.startScheduler());
    }

    @PostMapping("/schedule/stop")
    public ResponseEntity<MessageQueueScheduleSettingsDto> stopSchedule() {
        return ResponseEntity.ok(service.stopScheduler());
    }
}
