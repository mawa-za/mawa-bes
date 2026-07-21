package za.co.mawa.bes.controller.v2;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.service.v2.NumberRangeConfigurationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v2/number-range-configuration")
public class NumberRangeConfigurationControllerV2 {
    private final NumberRangeConfigurationService service;

    public NumberRangeConfigurationControllerV2(NumberRangeConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/sequences")
    public ResponseEntity<List<NumberSequenceResponseDto>> listSequences(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(service.listSequences(query, active));
    }

    @GetMapping("/sequences/{id}")
    public ResponseEntity<NumberSequenceResponseDto> getSequence(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSequence(id));
    }

    @PostMapping("/sequences")
    public ResponseEntity<NumberSequenceResponseDto> createSequence(
            @RequestBody NumberSequenceCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSequence(request));
    }

    @PutMapping("/sequences/{id}")
    public ResponseEntity<NumberSequenceResponseDto> updateSequence(
            @PathVariable Long id,
            @RequestBody NumberSequenceUpdateRequestDto request
    ) {
        return ResponseEntity.ok(service.updateSequence(id, request));
    }

    @GetMapping("/allocations")
    public ResponseEntity<List<NumberRangeAllocationResponseDto>> listAllocations(
            @RequestParam(required = false) String seqType,
            @RequestParam(required = false) String deviceId
    ) {
        return ResponseEntity.ok(service.listAllocations(seqType, deviceId));
    }

    @GetMapping("/document-ranges")
    public ResponseEntity<List<LegacyNumberRangeConfigurationResponseDto>> listDocumentRanges(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(service.listLegacyRanges(query));
    }

    @PostMapping("/document-ranges")
    public ResponseEntity<LegacyNumberRangeConfigurationResponseDto> createDocumentRange(
            @RequestBody LegacyNumberRangeConfigurationRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLegacyRange(request));
    }

    @PutMapping("/document-ranges/{id}")
    public ResponseEntity<LegacyNumberRangeConfigurationResponseDto> updateDocumentRange(
            @PathVariable Integer id,
            @RequestBody LegacyNumberRangeConfigurationRequestDto request
    ) {
        return ResponseEntity.ok(service.updateLegacyRange(id, request));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<Map<String, Object>>> listAudit(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String rangeKey
    ) {
        return ResponseEntity.ok(service.listAudit(sourceType, rangeKey));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage() == null ? "Invalid number range configuration" : ex.getMessage()
        ));
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> conflict(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", HttpStatus.CONFLICT.value(),
                "message", ex.getMessage() == null ? "Number range configuration changed concurrently" : ex.getMessage()
        ));
    }
}
