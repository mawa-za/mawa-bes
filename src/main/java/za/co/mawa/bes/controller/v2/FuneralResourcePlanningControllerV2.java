package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.FuneralResourcePlanningService;

@RestController
@CrossOrigin
@RequestMapping("/v2/funeral-resource-planning")
public class FuneralResourcePlanningControllerV2 {
    private final FuneralResourcePlanningService service;
    public FuneralResourcePlanningControllerV2(FuneralResourcePlanningService service){this.service=service;}
    @GetMapping("/configuration") public ResponseEntity<?> configuration(){return ResponseEntity.ok(service.configuration());}
    @PutMapping("/configuration") public ResponseEntity<?> configure(@RequestBody FuneralResourcePlanningService.ConfigurationRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.updateConfiguration(request,userId));}
    @GetMapping("/packages/{packageId}/requirements") public ResponseEntity<?> requirements(@PathVariable String packageId){return ResponseEntity.ok(service.requirements(packageId));}
    @PostMapping("/packages/{packageId}/requirements") public ResponseEntity<?> addRequirement(@PathVariable String packageId,@RequestBody FuneralResourcePlanningService.RequirementRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.saveRequirement(packageId,null,request,userId));}
    @PutMapping("/packages/{packageId}/requirements/{id}") public ResponseEntity<?> updateRequirement(@PathVariable String packageId,@PathVariable String id,@RequestBody FuneralResourcePlanningService.RequirementRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.saveRequirement(packageId,id,request,userId));}
    @GetMapping("/plans") public ResponseEntity<?> plans(@RequestParam(required=false)String status,@RequestParam(required=false)String assignee,@RequestParam(required=false)String query){return ResponseEntity.ok(service.plans(status,assignee,query));}
    @GetMapping("/plans/{id}") public ResponseEntity<?> plan(@PathVariable String id){return ResponseEntity.ok(service.plan(id));}
    @PutMapping("/plans/{id}/assignment") public ResponseEntity<?> assign(@PathVariable String id,@RequestBody FuneralResourcePlanningService.AssignmentRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.assign(id,request,userId));}
    @PostMapping("/funerals/{funeralServiceId}/plan") public ResponseEntity<?> create(@PathVariable String funeralServiceId,@RequestHeader(value="X-User-Id",required=false)String userId){service.ensurePlan(funeralServiceId,userId);return ResponseEntity.ok().build();}
    @GetMapping("/items/{id}/available-assets") public ResponseEntity<?> assets(@PathVariable String id){return ResponseEntity.ok(service.availableAssets(id));}
    @PostMapping("/items/{id}/internal-allocations") public ResponseEntity<?> internal(@PathVariable String id,@RequestBody FuneralResourcePlanningService.InternalAllocationRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.allocateInternal(id,request,userId));}
    @PostMapping("/items/{id}/external-allocations") public ResponseEntity<?> external(@PathVariable String id,@RequestBody FuneralResourcePlanningService.ExternalAllocationRequest request,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.leaseExternal(id,request,userId));}
    @PostMapping("/plans/{id}/confirm-ready") public ResponseEntity<?> ready(@PathVariable String id,@RequestHeader(value="X-User-Id",required=false)String userId){return ResponseEntity.ok(service.confirmReady(id,userId));}
}
