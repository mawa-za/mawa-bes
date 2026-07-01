package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.payapp.CashupDepositRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupDepositResponse;
import za.co.mawa.bes.dto.v2.payapp.CashupRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupResponse;
import za.co.mawa.bes.dto.v2.payapp.CashupSummaryResponse;
import za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest;
import za.co.mawa.bes.service.v2.CashupService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/cashup")
@RequiredArgsConstructor
public class CashupControllerV2 {

@Autowired
@Qualifier("CashupServiceV2")
CashupService cashupService;

    /**
     * Offline app submits cashup to backend.
     */
    @PostMapping
    public ResponseEntity<CashupResponse> submitCashup(@RequestBody CashupRequest request) {
        return ResponseEntity.ok(cashupService.submitCashup(request));
    }

    /**
     * Allocate offline cashup numbers to a device.
     *
     * Example:
     * POST /pay-app/cashups/number-range?deviceId=DEVICE001&size=100
     */

    @GetMapping("/{id}")
    public ResponseEntity<CashupSummaryResponse> getCashup(@PathVariable String id) {
        return ResponseEntity.ok(cashupService.getCashup(id));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<CashupSummaryResponse>> getCashupsByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(cashupService.getCashupsByDevice(deviceId));
    }

    @GetMapping("/active")
    public ResponseEntity<CashupSummaryResponse> getActiveCashup(
            @RequestParam String deviceId,
            @RequestParam String userId
    ) {
        return cashupService.getActiveCashup(deviceId, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CashupSummaryResponse>> getAll() {
        return ResponseEntity.ok(
                cashupService.getAll()
        );
    }

    @GetMapping
    public ResponseEntity<List<CashupSummaryResponse>> getCashupsByUserAndDateRange(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(
                cashupService.getCashupsByUserAndDateRange(userId, fromDate, toDate)
        );
    }


    @PostMapping("/{id}/deposits")
    public ResponseEntity<CashupDepositResponse> createDeposit(
            @PathVariable String id,
            @RequestBody CashupDepositRequest request
    ) {
        return ResponseEntity.ok(cashupService.createDeposit(id, request));
    }

    @GetMapping("/{id}/deposits")
    public ResponseEntity<List<CashupDepositResponse>> getDeposits(@PathVariable String id) {
        return ResponseEntity.ok(cashupService.getDeposits(id));
    }

    @DeleteMapping("/{id}/deposits/{depositId}")
    public ResponseEntity<Void> deleteDeposit(
            @PathVariable String id,
            @PathVariable String depositId
    ) {
        cashupService.deleteDeposit(id, depositId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<CashupResponse> submitForApproval(
            @PathVariable String id,
            @RequestBody(required = false) CashupSubmitForApprovalRequest request
    ) {
        return ResponseEntity.ok(cashupService.submitForApproval(id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CashupResponse> approveCashup(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String approvedBy = body != null ? body.getOrDefault("approvedBy", "system") : "system";
        return ResponseEntity.ok(cashupService.approveCashup(id, approvedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CashupResponse> rejectCashup(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String rejectedBy = body != null ? body.getOrDefault("rejectedBy", "system") : "system";
        String reason = body != null ? body.getOrDefault("reason", null) : null;

        return ResponseEntity.ok(cashupService.rejectCashup(id, rejectedBy, reason));
    }
}