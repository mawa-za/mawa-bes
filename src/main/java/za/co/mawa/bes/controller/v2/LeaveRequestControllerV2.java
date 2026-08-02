package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.service.v2.LeaveRequestV2Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin
@RestController
@RequestMapping("/v2/leave-request")
public class LeaveRequestControllerV2 {
    private final LeaveRequestV2Service service;

    public LeaveRequestControllerV2(LeaveRequestV2Service service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public ResponseEntity<LeaveRequestPreviewDto> preview(@RequestBody LeaveRequestV2CreateRequestDto request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping
    public ResponseEntity<LeaveRequestV2ResponseDto> create(@RequestBody LeaveRequestV2CreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestV2ResponseDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeePartnerId,
            @RequestParam(required = false) String approverPartnerId,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(service.search(status, employeePartnerId, approverPartnerId, leaveType, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestV2ResponseDto> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveRequestV2ResponseDto> update(
            @PathVariable String id,
            @RequestBody LeaveRequestV2UpdateRequestDto request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<LeaveRequestV2ResponseDto> submit(@PathVariable String id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestV2ResponseDto> cancel(
            @PathVariable String id,
            @RequestBody(required = false) LeaveRequestV2CancelRequestDto request) {
        return ResponseEntity.ok(service.cancel(id, request == null ? null : request.getReason()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
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
