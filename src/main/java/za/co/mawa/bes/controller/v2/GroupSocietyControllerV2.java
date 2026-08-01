package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.group.GroupSocietyAdjustmentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyContactRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyMemberRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyStatusChangeRequest;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;
import za.co.mawa.bes.service.v2.GroupSocietyService;
import za.co.mawa.bes.service.v2.GroupSocietyPaymentService;
import za.co.mawa.bes.service.v2.GroupSocietyApprovalService;
import za.co.mawa.bes.service.v2.GroupSocietyAgreementService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/v2/group-society")
public class GroupSocietyControllerV2 {

    private final GroupSocietyService groupSocietyService;
    private final GroupSocietyPaymentService paymentService;
    private final GroupSocietyApprovalService approvalService;
    private final GroupSocietyAgreementService agreementService;

    public GroupSocietyControllerV2(@Qualifier("GroupSocietyServiceV2") GroupSocietyService groupSocietyService,
                                    GroupSocietyPaymentService paymentService,
                                    GroupSocietyApprovalService approvalService,
                                    GroupSocietyAgreementService agreementService) {
        this.groupSocietyService = groupSocietyService;
        this.paymentService = paymentService;
        this.approvalService = approvalService;
        this.agreementService = agreementService;
    }

    @GetMapping
    public ResponseEntity<List<GroupSocietyEntity>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String societyType
    ) {
        return ResponseEntity.ok(groupSocietyService.getAll(status, societyType));
    }

    @GetMapping("/master-data")
    public ResponseEntity<?> getMasterData(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(groupSocietyService.getMasterData(status));
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
    public ResponseEntity<GroupSocietyEntity> activate(@PathVariable String id,
            @RequestBody(required = false) GroupSocietyStatusChangeRequest request) {
        return ResponseEntity.ok(approvalService.requestStatus(id, "ACTIVE",
                request == null ? new GroupSocietyStatusChangeRequest() : request));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<GroupSocietyEntity> suspend(@PathVariable String id,
            @RequestBody GroupSocietyStatusChangeRequest request) {
        return ResponseEntity.ok(approvalService.requestStatus(id, "SUSPENDED", request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<GroupSocietyEntity> close(@PathVariable String id,
            @RequestBody GroupSocietyStatusChangeRequest request) {
        return ResponseEntity.ok(approvalService.requestStatus(id, "CLOSED", request));
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
    public ResponseEntity<?> recordPayment(
            @PathVariable String id,
            @RequestBody GroupSocietyPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.createPayment(id, request));
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<GroupSocietyAccountTxnEntity> adjustBalance(
            @PathVariable String id,
            @RequestBody GroupSocietyAdjustmentRequest request
    ) {
        return ResponseEntity.ok(approvalService.requestAdjustment(id, request));
    }

    @GetMapping(value = "/{id}/agreement", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> agreement(@PathVariable String id) {
        byte[] pdf = agreementService.generate(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=group-society-agreement-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<List<GroupSocietyAccountTxnEntity>> getStatement(
            @PathVariable String id,
            @RequestParam(required = false) String period
    ) {
        return ResponseEntity.ok(groupSocietyService.getStatement(id, period));
    }
}
