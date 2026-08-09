package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.service.v2.EmployeeBankingDetailsService;
import za.co.mawa.bes.service.v2.EmploymentLifecycleService;
import za.co.mawa.bes.service.v2.EmploymentV2Service;
import za.co.mawa.bes.utils.Conversion;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin
@RestController
@RequestMapping("/v2/employment")
public class EmploymentControllerV2 {
    private final EmploymentV2Service employmentService;
    private final EmploymentLifecycleService lifecycleService;
    private final EmployeeBankingDetailsService employeeBankingDetailsService;

    public EmploymentControllerV2(
            EmploymentV2Service employmentService,
            EmploymentLifecycleService lifecycleService,
            EmployeeBankingDetailsService employeeBankingDetailsService) {
        this.employmentService = employmentService;
        this.lifecycleService = lifecycleService;
        this.employeeBankingDetailsService = employeeBankingDetailsService;
    }

    /** Backward-compatible route; this now creates an approval request instead of a live employment record. */
    @PostMapping
    public ResponseEntity<EmploymentActionResponseDto> requestHire(@RequestBody EmploymentActionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lifecycleService.requestHire(request));
    }

    @PostMapping("/actions/hire")
    public ResponseEntity<EmploymentActionResponseDto> requestHireAction(@RequestBody EmploymentActionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lifecycleService.requestHire(request));
    }

    @PostMapping("/{id}/actions/{actionType}")
    public ResponseEntity<EmploymentActionResponseDto> requestAction(
            @PathVariable String id,
            @PathVariable String actionType,
            @RequestBody EmploymentActionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lifecycleService.requestAction(id, actionType, request));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<EmploymentActionResponseDto>> actions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String actionType) {
        return ResponseEntity.ok(lifecycleService.listActions(status, actionType));
    }

    @GetMapping("/actions/{id}")
    public ResponseEntity<EmploymentActionResponseDto> action(@PathVariable String id) {
        return ResponseEntity.ok(lifecycleService.getAction(id));
    }

    @GetMapping("/history")
    public ResponseEntity<List<EmploymentHistoryDto>> history(@RequestParam(required = false) String employmentId) {
        return ResponseEntity.ok(lifecycleService.history(employmentId));
    }

    @GetMapping
    public ResponseEntity<List<EmploymentDto>> search(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String employeeNumber) {
        EmploymentSearchDto search = new EmploymentSearchDto();
        if (hasText(startDate)) search.setStartDate(Conversion.stringToDate(startDate));
        if (hasText(endDate)) search.setEndDate(Conversion.stringToDate(endDate));
        search.setBranch(trimToNull(branch)); search.setDepartment(trimToNull(department));
        search.setPosition(trimToNull(position)); search.setType(trimToNull(type));
        search.setStatus(trimToNull(status)); search.setPartnerId(trimToNull(partnerId));
        search.setEmployeeNumber(trimToNull(employeeNumber));
        return ResponseEntity.ok(employmentService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmploymentDto> get(@PathVariable String id) {
        return ResponseEntity.ok(employmentService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentDto> update(@PathVariable String id, @RequestBody EmploymentEditDto request) {
        return ResponseEntity.ok(employmentService.update(id, request));
    }

    @PutMapping("/{id}/terminate")
    public ResponseEntity<Map<String, String>> terminateLegacy(@PathVariable String id) {
        throw new IllegalStateException("Termination now requires an approval request. Use /v2/employment/" + id + "/actions/terminate");
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendLegacy(@PathVariable String id) {
        throw new IllegalStateException("Suspension now requires an approval request. Use /v2/employment/" + id + "/actions/suspend");
    }

    @PutMapping("/{id}/rehire")
    public ResponseEntity<Map<String, String>> rehireLegacy(@PathVariable String id) {
        throw new IllegalStateException("Rehire now requires an approval request. Use /v2/employment/" + id + "/actions/rehire");
    }

    @GetMapping("/employees")
    public ResponseEntity<List<PartnerDto>> employees() {
        return ResponseEntity.ok(employmentService.employees());
    }

    @GetMapping("/{id}/bank-details")
    public ResponseEntity<PartnerBankAccountGetDto> bankDetails(@PathVariable String id) {
        return ResponseEntity.ok(employeeBankingDetailsService.get(id));
    }

    @PostMapping("/{id}/bank-details/submit-for-approval")
    public ResponseEntity<ApprovalRequestResponse> submitBankDetails(
            @PathVariable String id,
            @RequestBody PartnerBankAccountDto request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(employeeBankingDetailsService.submit(id, request, userId));
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

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String trimToNull(String value) { return hasText(value) ? value.trim() : null; }
}
