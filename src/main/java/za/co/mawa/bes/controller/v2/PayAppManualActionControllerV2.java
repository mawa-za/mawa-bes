package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.PayAppManualActionService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/pay-app/manual-actions")
public class PayAppManualActionControllerV2 {
    private final PayAppManualActionService service;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> request,
                                    @RequestHeader HttpHeaders headers) throws Exception {
        return ResponseEntity.accepted().body(service.submit(request, headers));
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "ATTENTION_REQUIRED") String status,
                                          @RequestParam(defaultValue = "") String search) {
        return service.list(status, search);
    }
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable String id){return service.get(id);}
    @GetMapping("/reconcile") public Map<String,Object> reconcile(@RequestParam String deviceId,@RequestParam String entityType,@RequestParam String localRecordId){return service.reconcile(deviceId,entityType,localRecordId);}
    @PutMapping("/{id}/payload") public Map<String,Object> correct(@PathVariable String id,@RequestBody Map<String,Object> request)throws Exception{return service.correct(id,request);}
    @PostMapping("/{id}/process") public Map<String,Object> process(@PathVariable String id,@RequestHeader HttpHeaders headers)throws Exception{return service.process(id,headers);}
    @PostMapping("/{id}/reject") public Map<String,Object> reject(@PathVariable String id,@RequestBody Map<String,Object> request){return service.reject(id,request.get("notes")==null?"":request.get("notes").toString());}
}
