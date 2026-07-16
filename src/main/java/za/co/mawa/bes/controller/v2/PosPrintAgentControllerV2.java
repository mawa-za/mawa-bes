package za.co.mawa.bes.controller.v2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.*;
import za.co.mawa.bes.service.v2.PosPrintingService;
import java.util.List;

@RestController @CrossOrigin @RequiredArgsConstructor @RequestMapping("/v2/pos-print-agents")
public class PosPrintAgentControllerV2 {
 private final PosPrintingService service;
 @PostMapping("/enroll") public AgentEnrollResponse enroll(@RequestBody AgentEnrollRequest r,HttpServletRequest req){return service.enroll(r,clientIp(req));}
 @PostMapping("/{agentId}/heartbeat") public void heartbeat(@PathVariable String agentId,@RequestHeader("X-Mawa-Agent-Secret") String secret,@RequestBody(required=false) HeartbeatRequest r,HttpServletRequest req){service.heartbeat(agentId,secret,r,clientIp(req));}
 @PutMapping("/{agentId}/printers") public List<PrinterResponse> printers(@PathVariable String agentId,@RequestHeader("X-Mawa-Agent-Secret") String secret,@RequestBody PrinterSyncRequest r){return service.syncPrinters(agentId,secret,r);}
 @PostMapping("/{agentId}/jobs/claim") public ResponseEntity<PrintJobResponse> claim(@PathVariable String agentId,@RequestHeader("X-Mawa-Agent-Secret") String secret){PrintJobResponse j=service.claim(agentId,secret);return j==null?ResponseEntity.noContent().build():ResponseEntity.ok(j);}
 @PostMapping("/{agentId}/jobs/{jobId}/spooled") public void spooled(@PathVariable String agentId,@PathVariable String jobId,@RequestHeader("X-Mawa-Agent-Secret") String secret,@RequestBody JobResultRequest r){service.markSpooled(agentId,secret,jobId,r);}
 @PostMapping("/{agentId}/jobs/{jobId}/failed") public void failed(@PathVariable String agentId,@PathVariable String jobId,@RequestHeader("X-Mawa-Agent-Secret") String secret,@RequestBody JobResultRequest r){service.markFailed(agentId,secret,jobId,r);}
 private String clientIp(HttpServletRequest r){String f=r.getHeader("X-Forwarded-For");return f==null||f.isBlank()?r.getRemoteAddr():f.split(",")[0].trim();}
}
