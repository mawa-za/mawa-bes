package za.co.mawa.bes.controller.v2;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import za.co.mawa.bes.service.v2.ThirdPartyFuneralUnderwritingService; import java.util.*;
@RestController @RequestMapping("/v2/funeral-underwriting") @RequiredArgsConstructor
public class ThirdPartyFuneralUnderwritingController {
 private final ThirdPartyFuneralUnderwritingService service;
 @GetMapping("/underwriters") public List<Map<String,Object>> underwriters(){return service.underwriters();}
 @PostMapping("/underwriters") public Map<String,Object> saveUnderwriter(@RequestBody Map<String,Object>b){return service.saveUnderwriter(b);}
 @GetMapping("/covers") public List<Map<String,Object>> covers(@RequestParam(required=false)String status){return service.covers(status);}
 @GetMapping("/covers/{id}") public Map<String,Object> cover(@PathVariable String id){return service.getCover(id);}
 @PostMapping("/covers") public Map<String,Object> saveCover(@RequestBody Map<String,Object>b){return service.saveCover(b);}
 @PostMapping("/covers/{id}/decision") public Map<String,Object> decide(@PathVariable String id,@RequestBody Map<String,Object>b){return service.decide(id,b);}
}
