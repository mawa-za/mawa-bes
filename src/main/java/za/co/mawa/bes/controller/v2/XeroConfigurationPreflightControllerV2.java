package za.co.mawa.bes.controller.v2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.xero.XeroOutboundConfigurationPreflightService;

import java.util.Map;

/** Separate endpoint to avoid changing existing Phase 45.1 reconciliation routes. */
@RestController
@RequestMapping("/v2/integrations/xero/configuration")
public class XeroConfigurationPreflightControllerV2 {
    private final XeroOutboundConfigurationPreflightService preflight;

    public XeroConfigurationPreflightControllerV2(XeroOutboundConfigurationPreflightService preflight) {
        this.preflight = preflight;
    }

    @GetMapping("/read-only-preflight")
    public Map<String, Object> readOnlyPreflight() {
        return preflight.assess();
    }
}
