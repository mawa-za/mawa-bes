package za.co.mawa.bes.controller.v2;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimCreateRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimListItemResponse;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimUpdateRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimsAttachRequest;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.service.v2.MembershipClaimService;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/v2/membership-claim")
public class MembershipClaimControllerV2 {

    private final MembershipClaimService membershipClaimService;
    private final ClaimFormGenerationService claimFormGenerationService;

    public MembershipClaimControllerV2(MembershipClaimService membershipClaimService, ClaimFormGenerationService claimFormGenerationService) {
        this.membershipClaimService = membershipClaimService;
        this.claimFormGenerationService = claimFormGenerationService;
    }

    @PostMapping
    public ResponseEntity<MembershipClaimResponse> create(
            @RequestBody MembershipClaimCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(membershipClaimService.create(request, userId));
    }

    @GetMapping
    public ResponseEntity<List<MembershipClaimResponse>> getAll() {
        return ResponseEntity.ok(membershipClaimService.getAll());
    }

    @GetMapping("/page")
    public ResponseEntity<Slice<MembershipClaimListItemResponse>> getPage(
            @RequestParam(required = false) MembershipClaimStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(membershipClaimService.getPage(status, query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembershipClaimResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(membershipClaimService.getById(id));
    }

    @GetMapping("/claim-no/{claimNo}")
    public ResponseEntity<MembershipClaimResponse> getByClaimNo(@PathVariable String claimNo) {
        return ResponseEntity.ok(membershipClaimService.getByClaimNo(claimNo));
    }

    @GetMapping("/membership/{membershipId}")
    public ResponseEntity<List<MembershipClaimResponse>> getByMembershipId(@PathVariable String membershipId) {
        return ResponseEntity.ok(membershipClaimService.getByMembershipId(membershipId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MembershipClaimResponse>> getByStatus(@PathVariable MembershipClaimStatus status) {
        return ResponseEntity.ok(membershipClaimService.getByStatus(status));
    }

    @GetMapping("/type/{claimType}")
    public ResponseEntity<List<MembershipClaimResponse>> getByClaimType(@PathVariable MembershipClaimType claimType) {
        return ResponseEntity.ok(membershipClaimService.getByClaimType(claimType));
    }

    @GetMapping("/deceased-partner/{deceasedPartnerId}")
    public ResponseEntity<List<MembershipClaimResponse>> getByDeceasedPartnerId(
            @PathVariable String deceasedPartnerId
    ) {
        return ResponseEntity.ok(membershipClaimService.getByDeceasedPartnerId(deceasedPartnerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembershipClaimResponse> update(
            @PathVariable String id,
            @RequestBody MembershipClaimUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(membershipClaimService.update(id, request, userId));
    }

    @PostMapping("/{id}/claim-form")
    public ResponseEntity<byte[]> generateClaimForm(@PathVariable String id) {
        byte[] pdf = claimFormGenerationService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=claim-form-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/claim-form")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable String id) {
        return generateClaimForm(id);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<MembershipClaimResponse> submit(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(membershipClaimService.submit(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MembershipClaimResponse> cancel(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(membershipClaimService.cancel(id, userId));
    }

    @PostMapping("/{parentClaimId}/linked-claims")
    public ResponseEntity<MembershipClaimResponse> attachClaims(
            @PathVariable String parentClaimId,
            @RequestBody MembershipClaimsAttachRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(
                membershipClaimService.attachClaimsToCombination(
                        parentClaimId,
                        request,
                        userId
                )
        );
    }

    @DeleteMapping("/{parentClaimId}/linked-claims/{linkedClaimId}")
    public ResponseEntity<MembershipClaimResponse> detachClaim(
            @PathVariable String parentClaimId,
            @PathVariable String linkedClaimId
    ) {
        return ResponseEntity.ok(
                membershipClaimService.detachClaimFromCombination(
                        parentClaimId,
                        linkedClaimId
                )
        );
    }
}
