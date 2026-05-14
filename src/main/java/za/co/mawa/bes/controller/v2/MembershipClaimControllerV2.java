package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimCreateRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimUpdateRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimsAttachRequest;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.service.v2.MembershipClaimService;

import java.util.List;

@RestController
@RequestMapping("/v2/membership-claim")
public class MembershipClaimControllerV2 {

    private final MembershipClaimService membershipClaimService;

    public MembershipClaimControllerV2(MembershipClaimService membershipClaimService) {
        this.membershipClaimService = membershipClaimService;
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
