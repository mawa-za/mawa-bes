package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.AssetRegisterService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/assets")
public class AssetRegisterControllerV2 {
    private final AssetRegisterService service;
    public AssetRegisterControllerV2(AssetRegisterService service){this.service=service;}

    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard(){return ResponseEntity.ok(service.dashboard());}
    @GetMapping public ResponseEntity<List<Map<String,Object>>> list(@RequestParam(required=false) String query,@RequestParam(required=false) String status,@RequestParam(required=false) String category,@RequestParam(required=false) String custodianPartnerId){return ResponseEntity.ok(service.list(query,status,category,custodianPartnerId));}
    @GetMapping("/{id}") public ResponseEntity<Map<String,Object>> get(@PathVariable String id){return ResponseEntity.ok(service.get(id));}
    @PostMapping public ResponseEntity<Map<String,Object>> create(@RequestBody AssetRegisterService.AssetRequest request,@RequestHeader(value="X-User-Id",required=false) String userId){return ResponseEntity.ok(service.create(request,userId));}
    @PutMapping("/{id}") public ResponseEntity<Map<String,Object>> update(@PathVariable String id,@RequestBody AssetRegisterService.AssetRequest request,@RequestHeader(value="X-User-Id",required=false) String userId){return ResponseEntity.ok(service.update(id,request,userId));}
    @PostMapping("/{id}/assign") public ResponseEntity<Map<String,Object>> assign(@PathVariable String id,@RequestBody AssetRegisterService.AssignmentRequest request,@RequestHeader(value="X-User-Id",required=false) String userId){return ResponseEntity.ok(service.assign(id,request,userId));}
    @PostMapping("/{id}/dispose") public ResponseEntity<Map<String,Object>> dispose(@PathVariable String id,@RequestBody AssetRegisterService.DisposalRequest request,@RequestHeader(value="X-User-Id",required=false) String userId){return ResponseEntity.ok(service.dispose(id,request,userId));}
}
