package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import za.co.mawa.bes.configuration.context.UserContext;
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
            @RequestBody ApprovalSubmitRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        request.setRequesterId(currentUser(headerUserId, request.getRequesterId()));
        return approvalService.submitForApproval(request);
    }

    @PostMapping("/{approvalRequestId}/approve")
    public ApprovalRequestResponse approve(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        request.setActionBy(currentUser(headerUserId, request.getActionBy()));
        return approvalService.approve(approvalRequestId, request);
    }

    @PostMapping("/{approvalRequestId}/reject")
    public ApprovalRequestResponse reject(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        request.setActionBy(currentUser(headerUserId, request.getActionBy()));
        return approvalService.reject(approvalRequestId, request);
    }

    @PostMapping("/{approvalRequestId}/cancel")
    public ApprovalRequestResponse cancel(
            @PathVariable String approvalRequestId,
            @RequestBody ApprovalDecisionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        request.setActionBy(currentUser(headerUserId, request.getActionBy()));
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
        return approvalService.search(status, approvalType, requesterId);
    }

    private String currentUser(String headerUserId, String fallback) {
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (headerUserId != null && !headerUserId.isBlank()) return headerUserId;
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) {
            return UserContext.getCurrentUser();
        }
        if (fallback != null && !fallback.isBlank()) return fallback;
        throw new RuntimeException("Current user could not be determined");
    }

}