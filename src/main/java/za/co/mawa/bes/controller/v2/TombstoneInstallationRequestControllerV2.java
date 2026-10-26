package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneInstallationDtos;
import za.co.mawa.bes.service.v2.tombstone.TombstoneInstallationRequestService;
import java.util.Map;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2/tombstones/installation-requests")
public class TombstoneInstallationRequestControllerV2 {
    private final TombstoneInstallationRequestService service;

    @GetMapping("/configuration") public ResponseEntity<?> configuration() {
        return ResponseEntity.ok(service.configuration());
    }
    @GetMapping("/cover") public ResponseEntity<?> cover(@RequestParam String identityNumber,
            @RequestParam(required=false) java.time.LocalDate eventDate) {
        return ResponseEntity.ok(service.checkMembership(identityNumber, eventDate));
    }
    @GetMapping public ResponseEntity<?> list() { return ResponseEntity.ok(service.list()); }
    @GetMapping("/{id}") public ResponseEntity<?> get(@PathVariable String id) { return ResponseEntity.ok(service.get(id)); }

    @PostMapping public ResponseEntity<?> create(@RequestBody TombstoneInstallationDtos.Request request,
            @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.status(201).body(service.create(request, actor));
    }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable String id,
            @RequestBody TombstoneInstallationDtos.Request request,
            @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.update(id, request, actor));
    }
    @PostMapping("/{id}/claims") public ResponseEntity<?> claims(@PathVariable String id,
            @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.initiateClaims(id, actor));
    }
    @GetMapping("/{id}/claims/{claimId}/form") public ResponseEntity<byte[]> form(@PathVariable String id,
            @PathVariable String claimId, @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tombstone-claim-"+claimId+".pdf")
                .body(service.claimForm(id, claimId, actor));
    }
    @PostMapping("/{id}/claims/{claimId}/submit") public ResponseEntity<?> submit(@PathVariable String id,
            @PathVariable String claimId, @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.submitClaim(id, claimId, actor));
    }
    @PostMapping("/{id}/generate") public ResponseEntity<?> generate(@PathVariable String id,
            @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.generate(id, actor));
    }
    @PostMapping("/{id}/funding") public ResponseEntity<?> funding(@PathVariable String id,
            @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.allocateApprovedFunding(id, actor));
    }
    @PostMapping("/{id}/cancel") public ResponseEntity<?> cancel(@PathVariable String id,
            @RequestBody Map<String,String> body, @RequestHeader(value="X-User-Id", required=false) String actor) {
        return ResponseEntity.ok(service.cancel(id, body.get("reason"), actor));
    }
}
