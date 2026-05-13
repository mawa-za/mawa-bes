package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.dto.v2.group.GroupSocietyAdjustmentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyClaimDebitRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyContactRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyMemberRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyRequest;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;
import za.co.mawa.bes.service.v2.GroupSocietyService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/v2/group-societY")
public class GroupSocietyControllerV2 {

    private final GroupSocietyService groupSocietyService;

    public GroupSocietyControllerV2(GroupSocietyService groupSocietyService) {
        this.groupSocietyService = groupSocietyService;
    }

    @GetMapping
    public ResponseEntity<List<GroupSocietyEntity>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String societyType
    ) {
        return ResponseEntity.ok(groupSocietyService.getAll(status, societyType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupSocietyEntity> getById(@PathVariable String id) {
        return ResponseEntity.ok(groupSocietyService.getById(id));
    }

    @GetMapping("/by-group-no/{groupNo}")
    public ResponseEntity<GroupSocietyEntity> getByGroupNo(@PathVariable String groupNo) {
        return ResponseEntity.ok(groupSocietyService.getByGroupNo(groupNo));
    }

    @GetMapping("/by-partner/{partnerId}")
    public ResponseEntity<GroupSocietyEntity> getByPartnerId(@PathVariable String partnerId) {
        return ResponseEntity.ok(groupSocietyService.getByPartnerId(partnerId));
    }

    @PostMapping
    public ResponseEntity<GroupSocietyEntity> create(@RequestBody GroupSocietyRequest request) {
        return ResponseEntity.ok(groupSocietyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupSocietyEntity> update(
            @PathVariable String id,
            @RequestBody GroupSocietyRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.update(id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<GroupSocietyEntity> activate(@PathVariable String id) {
        return ResponseEntity.ok(groupSocietyService.activate(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<GroupSocietyEntity> suspend(@PathVariable String id) {
        return ResponseEntity.ok(groupSocietyService.suspend(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<GroupSocietyEntity> close(@PathVariable String id) {
        return ResponseEntity.ok(groupSocietyService.close(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        groupSocietyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    public ResponseEntity<List<GroupSocietyContactEntity>> getContacts(@PathVariable String id) {
        return ResponseEntity.ok(groupSocietyService.getContacts(id));
    }

    @PostMapping("/{id}/contacts")
    public ResponseEntity<GroupSocietyContactEntity> addContact(
            @PathVariable String id,
            @RequestBody GroupSocietyContactRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.addContact(id, request));
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable String contactId) {
        groupSocietyService.deleteContact(contactId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupSocietyMemberEntity>> getMembers(
            @PathVariable String id,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(groupSocietyService.getMembers(id, status));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupSocietyMemberEntity> addMember(
            @PathVariable String id,
            @RequestBody GroupSocietyMemberRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.addMember(id, request));
    }

    @PostMapping("/{id}/members/{memberId}/remove")
    public ResponseEntity<GroupSocietyMemberEntity> removeMember(
            @PathVariable String id,
            @PathVariable String memberId
    ) {
        return ResponseEntity.ok(groupSocietyService.removeMember(id, memberId));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<GroupSocietyAccountTxnEntity> recordPayment(
            @PathVariable String id,
            @RequestBody GroupSocietyPaymentRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.recordPayment(id, request));
    }

    @PostMapping("/{id}/claims/debit")
    public ResponseEntity<GroupSocietyAccountTxnEntity> debitClaim(
            @PathVariable String id,
            @RequestBody GroupSocietyClaimDebitRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.debitClaim(id, request));
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<GroupSocietyAccountTxnEntity> adjustBalance(
            @PathVariable String id,
            @RequestBody GroupSocietyAdjustmentRequest request
    ) {
        return ResponseEntity.ok(groupSocietyService.adjustBalance(id, request));
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<List<GroupSocietyAccountTxnEntity>> getStatement(
            @PathVariable String id,
            @RequestParam(required = false) String period
    ) {
        return ResponseEntity.ok(groupSocietyService.getStatement(id, period));
    }
}