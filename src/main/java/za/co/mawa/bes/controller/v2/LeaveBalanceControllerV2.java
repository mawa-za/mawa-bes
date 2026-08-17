package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.service.v2.LeaveBalanceAdjustmentService;
import za.co.mawa.bes.service.v2.LeaveBalanceService;
import za.co.mawa.bes.service.v2.LeaveAccessService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin
@RestController
@RequestMapping("/v2/leave-balance")
public class LeaveBalanceControllerV2 {
    private final LeaveBalanceService balanceService;
    private final LeaveBalanceAdjustmentService adjustmentService;
    private final LeaveAccessService leaveAccessService;

    public LeaveBalanceControllerV2(
            LeaveBalanceService balanceService,
            LeaveBalanceAdjustmentService adjustmentService,
            LeaveAccessService leaveAccessService) {
        this.balanceService = balanceService;
        this.adjustmentService = adjustmentService;
        this.leaveAccessService = leaveAccessService;
    }

    @GetMapping
    public ResponseEntity<List<LeaveBalanceDto>> balances(
            @RequestParam(required = false) String employmentId,
            @RequestParam(required = false) String view) {
        if ("APPROVER".equalsIgnoreCase(view)) {
            if (employmentId != null && !employmentId.isBlank()) {
                leaveAccessService.assertCanApproveEmployment(employmentId);
                return ResponseEntity.ok(balanceService.listBalances(employmentId));
            }
            List<LeaveBalanceDto> balances = leaveAccessService.approvableEmploymentIds().stream()
                    .flatMap(id -> balanceService.listBalances(id).stream())
                    .toList();
            return ResponseEntity.ok(balances);
        }

        String ownEmploymentId = employmentId;
        if (ownEmploymentId == null || ownEmploymentId.isBlank()) {
            ownEmploymentId = leaveAccessService.currentEmployment(java.time.LocalDate.now()).getId();
        } else {
            leaveAccessService.assertOwnEmployment(ownEmploymentId);
        }
        return ResponseEntity.ok(balanceService.listBalances(ownEmploymentId));
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<LeaveLedgerDto>> ledger(
            @RequestParam String employmentId,
            @RequestParam(required = false) String view) {
        if ("APPROVER".equalsIgnoreCase(view)) {
            leaveAccessService.assertCanApproveEmployment(employmentId);
        } else {
            leaveAccessService.assertOwnEmployment(employmentId);
        }
        return ResponseEntity.ok(balanceService.listLedger(employmentId));
    }

    @GetMapping("/adjustments")
    public ResponseEntity<List<LeaveBalanceAdjustmentResponseDto>> adjustments() {
        return ResponseEntity.ok(adjustmentService.list());
    }

    @GetMapping("/adjustments/{id}")
    public ResponseEntity<LeaveBalanceAdjustmentResponseDto> adjustment(@PathVariable String id) {
        return ResponseEntity.ok(adjustmentService.get(id));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<LeaveBalanceAdjustmentResponseDto> requestAdjustment(@RequestBody LeaveBalanceAdjustmentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adjustmentService.requestAdjustment(request));
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
