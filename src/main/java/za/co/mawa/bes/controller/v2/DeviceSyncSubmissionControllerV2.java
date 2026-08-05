package za.co.mawa.bes.controller.v2;
import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import za.co.mawa.bes.dto.v2.devicesync.*; import za.co.mawa.bes.service.DeviceSyncSubmissionService;
import java.security.Principal;
@RestController @RequestMapping("v2/device-sync/submissions") @RequiredArgsConstructor
public class DeviceSyncSubmissionControllerV2 {
 private final DeviceSyncSubmissionService service;
 @PostMapping public ResponseEntity<DeviceSyncSubmissionDto> submit(@RequestBody DeviceSyncSubmitRequest r,@RequestHeader(value="X-User-Id",required=false)String uid,Principal p){return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.submit(r,user(uid,p)));}
 @GetMapping("/{id}") public DeviceSyncSubmissionDto get(@PathVariable String id){return service.get(id);}
 @PostMapping("/{id}/process") public DeviceSyncSubmissionDto process(@PathVariable String id,@RequestHeader HttpHeaders headers){return service.process(id,headers);}
 @PutMapping("/{id}/correction") public DeviceSyncSubmissionDto correct(@PathVariable String id,@RequestBody DeviceSyncCorrectionRequest r,@RequestHeader(value="X-User-Id",required=false)String uid,Principal p){return service.correct(id,r,user(uid,p));}
 @PostMapping("/{id}/reprocess") public DeviceSyncSubmissionDto reprocess(@PathVariable String id,@RequestHeader HttpHeaders headers){return service.process(id,headers);}
 private String user(String h,Principal p){return h!=null&&!h.isBlank()?h:p==null?"UNKNOWN":p.getName();}
}
