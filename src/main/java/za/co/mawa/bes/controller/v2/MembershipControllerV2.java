package za.co.mawa.bes.controller.v2;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.v2.MembershipDependentService;
import za.co.mawa.bes.service.v2.MembershipPlanService;
import za.co.mawa.bes.service.v2.MembershipService;
import za.co.mawa.bes.service.v2.MigrateService;

import java.util.List;

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

    public MembershipControllerV2(
            MembershipPlanService membershipPlanService,
            @Qualifier("MembershipServiceV2")
            MembershipService membershipService,
            MembershipDependentService membershipDependentService) {
        this.membershipPlanService = membershipPlanService;
        this.membershipService = membershipService;
        this.membershipDependentService = membershipDependentService;
    }

    // ------------------------------------------
    // Membership Plan Endpoints
    // ------------------------------------------
    @GetMapping("migrate")
    public ResponseEntity<?> migrate() {
        migrateService.migrateMemberships();
        return ResponseEntity.ok().build();
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
    public ResponseEntity<Page<MembershipEntity>> listMemberships(Pageable pageable,
                                                                  @RequestParam(required = false) List<String> memberId
                                                                  ) {
        Page<MembershipEntity> page = membershipService.getMembershipsByMemberId(memberId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping(value = "/all")
    public ResponseEntity<Page<MembershipEntity>> getMemberships(Pageable pageable) {
        return ResponseEntity.ok(membershipRepository.findAll(pageable));
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
    public ResponseEntity<MembershipDependentEntity> addDependent(
            @PathVariable String membershipId,
            @Valid @RequestBody MembershipDependentEntity dependent) {
        MembershipDependentEntity createdDependent = membershipDependentService.addDependent(membershipId, dependent);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDependent);
    }

    @PutMapping("/{membershipId}/dependents/{dependentId}")
    public ResponseEntity<MembershipDependentEntity> updateDependent(
            @PathVariable String membershipId,
            @PathVariable String dependentId,
            @Valid @RequestBody MembershipDependentEntity dependent) {
        return membershipDependentService.updateDependent(membershipId, dependentId, dependent)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{membershipId}/dependents/{dependentId}")
    public ResponseEntity<Void> deleteDependent(
            @PathVariable String membershipId,
            @PathVariable String dependentId) {
        if (membershipDependentService.deleteDependent(membershipId, dependentId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}