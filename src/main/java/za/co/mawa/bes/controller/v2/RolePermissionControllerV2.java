package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.RolePermissionService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("v2")
public class RolePermissionControllerV2 {
    private final RolePermissionService permissions;

    @GetMapping("/permissions")
    public List<Map<String, Object>> catalogue() {
        return permissions.catalogue();
    }

    @GetMapping("/role/{role}/permissions")
    public ResponseEntity<?> assigned(@PathVariable String role) {
        try {
            return ResponseEntity.ok(permissions.assigned(role));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/role/{role}/permissions")
    public ResponseEntity<?> replace(@PathVariable String role, @RequestBody List<String> codes) {
        try {
            return ResponseEntity.ok(permissions.replace(role, codes));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "ROLE_PERMISSION_CHANGE_DENIED", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
