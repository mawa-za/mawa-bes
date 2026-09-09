package za.co.mawa.bes.controller.v2;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.service.WorkcenterService;

import java.util.List;

@RestController
@RequestMapping("/v2/workcenters")
public class WorkcenterControllerV2 {
    private final WorkcenterService workcenterService;

    public WorkcenterControllerV2(WorkcenterService workcenterService) {
        this.workcenterService = workcenterService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WorkcenterDto>> getWorkcenters() {
        return ResponseEntity.ok(workcenterService.getAll());
    }
}
