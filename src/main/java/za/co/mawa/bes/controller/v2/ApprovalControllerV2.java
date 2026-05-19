package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.ApprovalDecisionRequest;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowCreateRequest;
import za.co.mawa.bes.entity.v2.ApprovalActionEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.v2.ApprovalService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("v2/approval")
@RequiredArgsConstructor
public class ApprovalControllerV2 {

    private final ApprovalService approvalService;

//    @PostMapping("/workflows")
//    public ApprovalWorkflowEntity createWorkflow(
//            @RequestBody ApprovalWorkflowCreateRequest request,
//            @RequestHeader(value = "X-User-Id", required = false) String userId
//    ) {
//        return approvalService.createWorkflow(request, userId);
//    }

    @PostMapping("/submit")
    public ApprovalRequestResponse submitForApproval(
            @RequestBody ApprovalSubmitRequest request
    ) {
        return approvalService.submitForApproval(request);
    }

    @PostMapping("/{approvalRequestId}/approve")
    public ApprovalRequestResponse approve(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request
    ) {
        return approvalService.approve(approvalRequestId, request);
    }

    @PostMapping("/{approvalRequestId}/reject")
    public ApprovalRequestResponse reject(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request
    ) {
        return approvalService.reject(approvalRequestId, request);
    }

    @PostMapping("/{approvalRequestId}/cancel")
    public ApprovalRequestResponse cancel(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request
    ) {
        return approvalService.cancel(approvalRequestId, request);
    }

    @GetMapping("/{approvalRequestId}")
    public ApprovalRequestResponse getById(
            @PathVariable String approvalRequestId
    ) {
        return approvalService.getById(approvalRequestId);
    }

    @GetMapping("/{approvalRequestId}/audit")
    public List<ApprovalActionEntity> getAuditTrail(
            @PathVariable String approvalRequestId
    ) {
        return approvalService.getAuditTrail(approvalRequestId);
    }

    @GetMapping
    public List<ApprovalRequestEntity> search(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) ApprovalType approvalType,
            @RequestParam(required = false) String requesterId
    ) {
        if (status != null) {
            return approvalService.getByStatus(status);
        }

        if (approvalType != null) {
            return approvalService.getByType(approvalType);
        }

        if (requesterId != null) {
            return approvalService.getByRequester(requesterId);
        }

        return approvalService.getByStatus(ApprovalStatus.IN_PROGRESS);
    }
}