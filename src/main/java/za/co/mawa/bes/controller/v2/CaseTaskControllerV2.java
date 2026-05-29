package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseTaskCreateRequest;
import za.co.mawa.bes.dto.v2.CaseTaskStatusUpdateRequest;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.service.v2.CaseTaskService;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseTaskControllerV2 {
    private final CaseTaskService caseTaskService;
    @PostMapping("/{caseId}/tasks") public CaseTaskEntity create(@PathVariable String caseId, @RequestBody CaseTaskCreateRequest request) { return caseTaskService.create(caseId, request, request.getAssignedTo()); }
    @GetMapping("/{caseId}/tasks") public List<CaseTaskEntity> findByCase(@PathVariable String caseId) { return caseTaskService.findByCaseId(caseId); }
    @GetMapping("/tasks/my") public List<CaseTaskEntity> myTasks(@RequestParam String userId) { return caseTaskService.findMyTasks(userId); }
    @PatchMapping("/tasks/{taskId}/status") public CaseTaskEntity updateStatus(@PathVariable String taskId, @RequestBody CaseTaskStatusUpdateRequest request) { return caseTaskService.updateStatus(taskId, request, request.getCompletedBy()); }
}
