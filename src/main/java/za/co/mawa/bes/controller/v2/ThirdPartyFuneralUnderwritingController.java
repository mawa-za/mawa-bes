package za.co.mawa.bes.controller.v2;

import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.ThirdPartyFuneralUnderwritingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v2/funeral-underwriting")
public class ThirdPartyFuneralUnderwritingController {
    private final ThirdPartyFuneralUnderwritingService service;

    public ThirdPartyFuneralUnderwritingController(ThirdPartyFuneralUnderwritingService service) {
        this.service = service;
    }

    @GetMapping("/underwriters")
    public List<Map<String, Object>> underwriters() { return service.underwriters(); }

    @PostMapping("/underwriters")
    public Map<String, Object> saveUnderwriter(@RequestBody Map<String, Object> body) {
        return service.saveUnderwriter(body);
    }

    @GetMapping("/eligible-parties")
    public List<Map<String, Object>> eligibleParties(@RequestParam(required = false) String query) {
        return service.eligibleParties(query);
    }

    @GetMapping("/covers")
    public List<Map<String, Object>> covers(@RequestParam(required = false) String status) {
        return service.covers(status);
    }

    @GetMapping("/covers/{id}")
    public Map<String, Object> cover(@PathVariable String id) { return service.getCover(id); }

    @PostMapping("/covers")
    public Map<String, Object> saveCover(@RequestBody Map<String, Object> body) {
        return service.saveCover(body);
    }

    @PostMapping("/covers/{id}/decision")
    public Map<String, Object> decide(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return service.decide(id, body);
    }
}
