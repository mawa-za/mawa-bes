package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.devicesync.*;
import za.co.mawa.bes.service.DeviceSyncSubmissionService;

import java.security.Principal;

@RestController
@RequestMapping("v2/device-sync/submissions")
@RequiredArgsConstructor
public class DeviceSyncSubmissionControllerV2 {
    private final DeviceSyncSubmissionService service;

    @PostMapping
    public ResponseEntity<DeviceSyncSubmissionDto> submit(@RequestBody DeviceSyncSubmitRequest request,
                                                           @RequestHeader(value="X-User-Id", required=false) String userId,
                                                           Principal principal) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.submit(request, user(userId, principal)));
    }

    @GetMapping
    public Page<DeviceSyncSubmissionDto> list(@RequestParam(required=false) String status,
                                               @RequestParam(required=false) String search,
                                               @RequestParam(defaultValue="0") int page,
                                               @RequestParam(defaultValue="50") int size) {
        return service.list(status, search, page, size);
    }

    @GetMapping("/{id}") public DeviceSyncSubmissionDto get(@PathVariable String id){ return service.get(id); }
    @PostMapping("/{id}/process") public DeviceSyncSubmissionDto process(@PathVariable String id,@RequestHeader HttpHeaders headers){ return service.process(id,headers); }
    @PutMapping("/{id}/correction") public DeviceSyncSubmissionDto correct(@PathVariable String id,@RequestBody DeviceSyncCorrectionRequest request,@RequestHeader(value="X-User-Id",required=false)String userId,Principal principal){ return service.correct(id,request,user(userId,principal)); }
    @PostMapping("/{id}/reprocess") public DeviceSyncSubmissionDto reprocess(@PathVariable String id,@RequestHeader HttpHeaders headers){ return service.process(id,headers); }
    @PostMapping("/{id}/cancel")
    public DeviceSyncSubmissionDto cancel(@PathVariable String id,
                                           @RequestBody DeviceSyncCancellationRequest request,
                                           @RequestHeader(value="X-User-Id",required=false) String userId,
                                           Principal principal) {
        return service.cancel(id, request, user(userId, principal));
    }
    @PostMapping("/cancel-by-key")
    public DeviceSyncSubmissionDto cancelByKey(
            @RequestBody DeviceSyncCancellationRequest request,
            @RequestHeader(value="X-User-Id",required=false) String userId,
            Principal principal) {
        return service.cancelByKey(request, user(userId, principal));
    }
    private String user(String header,Principal principal){ return header!=null&&!header.isBlank()?header:principal==null?"UNKNOWN":principal.getName(); }
}
