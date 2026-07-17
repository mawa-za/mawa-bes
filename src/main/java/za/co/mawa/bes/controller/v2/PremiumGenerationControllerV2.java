package za.co.mawa.bes.controller.v2;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import za.co.mawa.bes.service.v2.PremiumGenerationService;import java.util.Map;
@RestController @RequestMapping("/v2/premium-generation") @RequiredArgsConstructor
public class PremiumGenerationControllerV2 {private final PremiumGenerationService service;
 @GetMapping("/configuration") public Map<String,Object> get(){return service.configuration();}
 @PutMapping("/configuration") public Map<String,Object> save(@RequestBody Map<String,Object> r,@RequestHeader(value="X-User-Id",required=false)String u){return service.saveConfiguration(r,u);}
 @PostMapping("/backfill-six-periods") public Map<String,Object> backfill(@RequestHeader(value="X-User-Id",required=false)String u){return service.backfillSixPeriods(u==null?"SYSTEM":u);}
}
