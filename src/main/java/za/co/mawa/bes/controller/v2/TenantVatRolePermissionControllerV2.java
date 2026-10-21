package za.co.mawa.bes.controller.v2;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.TenantVatRolePermissionService;

@RestController
@RequestMapping("/v2/role")
public class TenantVatRolePermissionControllerV2 {
    private final TenantVatRolePermissionService service;
    public TenantVatRolePermissionControllerV2(TenantVatRolePermissionService service) {
        this.service = service;
    }

    @GetMapping("/{roleId}/vat-permissions")
    public Map<String, Object> get(@PathVariable String roleId) {
        return service.get(roleId);
    }

    @PutMapping("/{roleId}/vat-permissions")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String roleId,
            @RequestBody TenantVatRolePermissionService.UpdateRequest request) {
        return ResponseEntity.ok(service.update(roleId, request));
    }
}
