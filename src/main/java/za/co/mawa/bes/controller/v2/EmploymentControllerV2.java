package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmploymentCreateDto;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.EmploymentEditDto;
import za.co.mawa.bes.dto.EmploymentSearchDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
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

    public EmploymentControllerV2(EmploymentV2Service employmentService) {
        this.employmentService = employmentService;
    }

    @PostMapping
    public ResponseEntity<EmploymentDto> hire(@RequestBody EmploymentCreateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(employmentService.hire(request));
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
            @RequestParam(required = false) String employeeNumber) throws Exception {
        EmploymentSearchDto search = new EmploymentSearchDto();
        if (hasText(startDate)) search.setStartDate(Conversion.stringToDate(startDate));
        if (hasText(endDate)) search.setEndDate(Conversion.stringToDate(endDate));
        search.setBranch(trimToNull(branch));
        search.setDepartment(trimToNull(department));
        search.setPosition(trimToNull(position));
        search.setType(trimToNull(type));
        search.setStatus(trimToNull(status));
        search.setPartnerId(trimToNull(partnerId));
        search.setEmployeeNumber(trimToNull(employeeNumber));
        return ResponseEntity.ok(employmentService.search(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmploymentDto> get(@PathVariable String id) {
        return ResponseEntity.ok(employmentService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentDto> update(
            @PathVariable String id,
            @RequestBody EmploymentEditDto request) throws Exception {
        return ResponseEntity.ok(employmentService.update(id, request));
    }

    @PutMapping("/{id}/terminate")
    public ResponseEntity<EmploymentDto> terminate(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(employmentService.terminate(id));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<EmploymentDto> suspend(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(employmentService.suspend(id));
    }

    @PutMapping("/{id}/rehire")
    public ResponseEntity<EmploymentDto> rehire(
            @PathVariable String id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) throws Exception {
        return ResponseEntity.ok(employmentService.rehire(id, startDate, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        employmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employees")
    public ResponseEntity<List<PartnerDto>> employees() throws Exception {
        return ResponseEntity.ok(employmentService.employees());
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
