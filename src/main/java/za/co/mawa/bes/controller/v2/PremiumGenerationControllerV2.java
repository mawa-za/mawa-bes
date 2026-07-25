package za.co.mawa.bes.controller.v2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.service.v2.PremiumGenerationService;

import java.util.Map;

@RestController
@RequestMapping("/v2/premium-generation")
public class PremiumGenerationControllerV2 {

    private final PremiumGenerationService service;

    public PremiumGenerationControllerV2(PremiumGenerationService service) {
        this.service = service;
    }

    @GetMapping("/configuration")
    public Map<String, Object> getConfiguration() {
        return service.configuration();
    }

    @PutMapping("/configuration")
    public Map<String, Object> saveConfiguration(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return service.saveConfiguration(request, userId);
    }

    @PostMapping("/backfill-six-periods")
    public Map<String, Object> backfillSixPeriods(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return service.backfillSixPeriods(userId);
    }
}
