package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.dto.v2.approval.ApprovalWorkflowRequestDto;
import za.co.mawa.bes.dto.v2.approval.ApprovalWorkflowResponseDto;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.v2.ApprovalWorkflowConfigService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/approval-workflow")
public class ApprovalWorkflowConfigControllerV2 {

    private final ApprovalWorkflowConfigService approvalWorkflowConfigService;

    @PostMapping
    public ResponseEntity<ApprovalWorkflowResponseDto> create(
            @RequestBody ApprovalWorkflowRequestDto request
    ) {
        return ResponseEntity.ok(approvalWorkflowConfigService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApprovalWorkflowResponseDto> update(
            @PathVariable String id,
            @RequestBody ApprovalWorkflowRequestDto request
    ) {
        return ResponseEntity.ok(approvalWorkflowConfigService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalWorkflowResponseDto> getById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(approvalWorkflowConfigService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ApprovalWorkflowResponseDto>> getAll() {
        return ResponseEntity.ok(approvalWorkflowConfigService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ApprovalWorkflowResponseDto>> getActive() {
        return ResponseEntity.ok(approvalWorkflowConfigService.getActive());
    }

    @GetMapping("/type/{approvalType}")
    public ResponseEntity<ApprovalWorkflowResponseDto> getByApprovalType(
            @PathVariable ApprovalType approvalType
    ) {
        return ResponseEntity.ok(approvalWorkflowConfigService.getByApprovalType(approvalType));
    }

    @GetMapping("/type/{approvalType}/active")
    public ResponseEntity<ApprovalWorkflowResponseDto> getActiveByApprovalType(
            @PathVariable ApprovalType approvalType
    ) {
        return ResponseEntity.ok(approvalWorkflowConfigService.getActiveByApprovalType(approvalType));
    }

    @GetMapping("/applicable")
    public ResponseEntity<ApprovalWorkflowResponseDto> findApplicableWorkflow(
            @RequestParam ApprovalType approvalType,
            @RequestParam(required = false) BigDecimal amount
    ) {
        return ResponseEntity.ok(
                approvalWorkflowConfigService.findApplicableWorkflow(approvalType, amount)
        );
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable String id) {
        approvalWorkflowConfigService.activate(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        approvalWorkflowConfigService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        approvalWorkflowConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
