package za.co.mawa.bes.controller.v2;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.mawa.bes.dto.v2.sync.MembershipMasterDataDto;
import za.co.mawa.bes.dto.v2.MembershipResponseDto;
import za.co.mawa.bes.dto.v2.membership.change.MembershipChangeResponse;
import za.co.mawa.bes.dto.v2.membership.change.MembershipDependentAddRequest;
import za.co.mawa.bes.dto.v2.membership.change.MembershipDependentRemoveRequest;
import za.co.mawa.bes.dto.v2.membership.change.MembershipDependentReplaceRequest;
import za.co.mawa.bes.dto.v2.payapp.PayAppMasterDataSnapshotResponse;
import za.co.mawa.bes.dto.v2.payapp.PayAppMasterDataChangesResponse;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.v2.MembershipDependentService;
import za.co.mawa.bes.service.v2.MembershipChangeService;
import za.co.mawa.bes.service.v2.MembershipPlanService;
import za.co.mawa.bes.service.v2.MembershipService;
import za.co.mawa.bes.service.v2.MigrateService;
import za.co.mawa.bes.service.v2.PayAppMasterDataService;

import java.security.Principal;
import java.util.List;
import za.co.mawa.bes.configuration.context.UserContext;

@CrossOrigin
@RestController
@RequestMapping("v2/membership")
public class MembershipControllerV2 {
    @Autowired
    MigrateService migrateService;
    @Autowired
    MembershipRepository membershipRepository;
    private final MembershipPlanService membershipPlanService;
    private final MembershipService membershipService;
    private final MembershipDependentService membershipDependentService;
    private final MembershipChangeService membershipChangeService;
    private final PayAppMasterDataService payAppMasterDataService;

    public MembershipControllerV2(
            MembershipPlanService membershipPlanService,
            @Qualifier("MembershipServiceV2")
            MembershipService membershipService,
            MembershipDependentService membershipDependentService,
            MembershipChangeService membershipChangeService,
            PayAppMasterDataService payAppMasterDataService) {
        this.membershipPlanService = membershipPlanService;
        this.membershipService = membershipService;
        this.membershipDependentService = membershipDependentService;
        this.membershipChangeService = membershipChangeService;
        this.payAppMasterDataService = payAppMasterDataService;
    }

    // ------------------------------------------
    // Membership Plan Endpoints
    // ------------------------------------------
    @PostMapping("migrate")
    public ResponseEntity<?> migrate() {
        return ResponseEntity.ok(migrateService.migrateMemberships());
    }

    @GetMapping("migrate")
    public ResponseEntity<?> migrateLegacyGet() {
        return ResponseEntity.ok(migrateService.migrateMemberships());
    }

    @GetMapping("/plans")
    public ResponseEntity<Page<MembershipPlanEntity>> listMembershipPlans(Pageable pageable) {
        return ResponseEntity.ok(membershipPlanService.getAllPlans(pageable));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<MembershipPlanEntity> getMembershipPlan(@PathVariable String id) {
        return membershipPlanService.getPlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/plans")
    public ResponseEntity<MembershipPlanEntity> createMembershipPlan(@Valid @RequestBody MembershipPlanEntity membershipPlan) {
        MembershipPlanEntity createdPlan = membershipPlanService.createPlan(membershipPlan);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<MembershipPlanEntity> updateMembershipPlan(
            @PathVariable String id,
            @Valid @RequestBody MembershipPlanEntity membershipPlan) {
        return membershipPlanService.updatePlan(id, membershipPlan)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deleteMembershipPlan(@PathVariable String id) {
        if (membershipPlanService.deletePlan(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ------------------------------------------
    // Membership Endpoints
    // ------------------------------------------

    @GetMapping
    public ResponseEntity<Page<MembershipResponseDto>> listMemberships(
            Pageable pageable,
            @RequestParam(required = false) List<String> memberId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                membershipService.getMembershipResponsesByMemberId(memberId, status, pageable)
        );
    }

    @GetMapping(value = "/all")
    public ResponseEntity<Page<MembershipResponseDto>> getMemberships(
            Pageable pageable,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                membershipService.getAllMembershipResponses(status, pageable)
        );
    }

    /**
     * Bounded master-data feed used by MAWA Pay. A page contains the membership
     * and the minimal partner fields required for offline receipt lookup.
     */
    @GetMapping(value = "/master-data")
    public ResponseEntity<List<MembershipMasterDataDto>> getMasterData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "250") int size) {
        return ResponseEntity.ok(membershipService.getMasterData(page, size));
    }


    /**
     * Stable keyset-paged initial snapshot for MAWA Pay. The returned watermark
     * becomes the starting point for later incremental requests.
     */
    @GetMapping(value = "/master-data/snapshot")
    public ResponseEntity<PayAppMasterDataSnapshotResponse> getMasterDataSnapshot(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "500") int size) {
        return ResponseEntity.ok(payAppMasterDataService.snapshot(cursor, size));
    }

    /**
     * Watermark-based incremental feed containing upserts and tombstones.
     */
    @GetMapping(value = "/master-data/changes")
    public ResponseEntity<PayAppMasterDataChangesResponse> getMasterDataChanges(
            @RequestParam(defaultValue = "0") long after,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "500") int size) {
        return ResponseEntity.ok(payAppMasterDataService.changes(after, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembershipEntity> getMembership(@PathVariable String id) {
        return membershipService.getMembershipById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MembershipEntity> createMembership(@RequestBody MembershipEntity membership) {
        MembershipEntity createdMembership = membershipService.createMembership(membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMembership);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembershipEntity> updateMembership(
            @PathVariable String id,
            @Valid @RequestBody MembershipEntity membership) {
        return membershipService.updateMembership(id, membership)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembership(@PathVariable String id) {
        if (membershipService.deleteMembership(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ------------------------------------------
    // Membership Dependent Endpoints
    // ------------------------------------------

    @GetMapping("/{membershipId}/dependents")
    public ResponseEntity<List<MembershipDependentEntity>> listDependents(@PathVariable String membershipId) {
        return ResponseEntity.ok(membershipDependentService.getDependentsByMembershipId(membershipId));
    }

    @PostMapping("/{membershipId}/dependents")
    public ResponseEntity<MembershipChangeResponse> addDependent(
            @PathVariable String membershipId,
            @Valid @RequestBody MembershipDependentAddRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membershipChangeService.requestDependentAdd(
                        membershipId, request, actor(principal)));
    }

    @PutMapping("/{membershipId}/dependents/{dependentId}")
    public ResponseEntity<MembershipChangeResponse> replaceDependent(
            @PathVariable String membershipId,
            @PathVariable String dependentId,
            @Valid @RequestBody MembershipDependentReplaceRequest request,
            Principal principal) {
        return ResponseEntity.ok(membershipChangeService.requestDependentReplace(
                membershipId, dependentId, request, actor(principal)));
    }

    @PostMapping("/{membershipId}/dependents/{dependentId}/remove")
    public ResponseEntity<MembershipChangeResponse> removeDependent(
            @PathVariable String membershipId,
            @PathVariable String dependentId,
            @RequestBody MembershipDependentRemoveRequest request,
            Principal principal) {
        return ResponseEntity.ok(membershipChangeService.requestDependentRemove(
                membershipId, dependentId, request, actor(principal)));
    }

    /**
     * Compatibility endpoint for older clients. Removal still follows the
     * same one-month approval rule and is never a physical delete.
     */
    @DeleteMapping("/{membershipId}/dependents/{dependentId}")
    public ResponseEntity<MembershipChangeResponse> deleteDependent(
            @PathVariable String membershipId,
            @PathVariable String dependentId,
            Principal principal) {
        MembershipDependentRemoveRequest request = new MembershipDependentRemoveRequest();
        request.setReason("Dependent removed");
        return ResponseEntity.ok(membershipChangeService.requestDependentRemove(
                membershipId, dependentId, request, actor(principal)));
    }

    private String actor(Principal principal) {
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) {
            return UserContext.getCurrentUser();
        }
        return "SYSTEM";
    }

}