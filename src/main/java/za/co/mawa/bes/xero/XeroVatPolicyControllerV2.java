package za.co.mawa.bes.xero;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-scoped VAT policy maintenance. No financial posting or invoice mutation. */
@RestController
@RequestMapping("/v2/integrations/xero/vat-policy")
public class XeroVatPolicyControllerV2 {
    private final XeroVatPolicyService service;

    public XeroVatPolicyControllerV2(XeroVatPolicyService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> get() { return service.read(); }

    @PostMapping
    public Map<String, Object> create(@RequestBody XeroVatPolicyService.PolicyRequest request) {
        return service.create(request);
    }
}
