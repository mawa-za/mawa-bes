package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.service.v2.LeaveConfigurationService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin
@RestController
@RequestMapping("/v2/leave-configuration")
public class LeaveConfigurationControllerV2 {
    private final LeaveConfigurationService service;

    public LeaveConfigurationControllerV2(LeaveConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeDto>> leaveTypes(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(service.listLeaveTypes(activeOnly));
    }

    @PostMapping("/types")
    public ResponseEntity<LeaveTypeDto> createLeaveType(@RequestBody LeaveTypeDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveLeaveType(null, request));
    }

    @PutMapping("/types/{id}")
    public ResponseEntity<LeaveTypeDto> updateLeaveType(@PathVariable String id, @RequestBody LeaveTypeDto request) {
        return ResponseEntity.ok(service.saveLeaveType(id, request));
    }

    @DeleteMapping("/types/{id}")
    public ResponseEntity<Void> deactivateLeaveType(@PathVariable String id) {
        service.deactivateLeaveType(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendars")
    public ResponseEntity<List<WorkingCalendarDto>> calendars() {
        return ResponseEntity.ok(service.listCalendars());
    }

    @PostMapping("/calendars")
    public ResponseEntity<WorkingCalendarDto> createCalendar(@RequestBody WorkingCalendarDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveCalendar(null, request));
    }

    @PutMapping("/calendars/{id}")
    public ResponseEntity<WorkingCalendarDto> updateCalendar(@PathVariable String id, @RequestBody WorkingCalendarDto request) {
        return ResponseEntity.ok(service.saveCalendar(id, request));
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<LeaveProfileDto>> profiles() {
        return ResponseEntity.ok(service.listProfiles());
    }

    @PostMapping("/profiles")
    public ResponseEntity<LeaveProfileDto> createProfile(@RequestBody LeaveProfileDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProfile(null, request));
    }

    @PutMapping("/profiles/{id}")
    public ResponseEntity<LeaveProfileDto> updateProfile(@PathVariable String id, @RequestBody LeaveProfileDto request) {
        return ResponseEntity.ok(service.saveProfile(id, request));
    }

    @GetMapping("/employee-assignments")
    public ResponseEntity<List<LeaveProfileAssignmentDto>> employeeAssignments(@RequestParam(required = false) String employmentId) {
        return ResponseEntity.ok(service.listEmployeeAssignments(employmentId));
    }

    @PostMapping("/employee-assignments")
    public ResponseEntity<LeaveProfileAssignmentDto> assignEmployee(@RequestBody LeaveProfileAssignmentDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assignEmployee(request));
    }

    @GetMapping("/position-assignments")
    public ResponseEntity<List<LeaveProfileAssignmentDto>> positionAssignments() {
        return ResponseEntity.ok(service.listPositionAssignments());
    }

    @PostMapping("/position-assignments")
    public ResponseEntity<LeaveProfileAssignmentDto> assignPosition(@RequestBody LeaveProfileAssignmentDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assignPosition(request));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}
