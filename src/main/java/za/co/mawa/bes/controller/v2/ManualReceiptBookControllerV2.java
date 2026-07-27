package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.manualreceipt.ManualReceiptBookRequest;
import za.co.mawa.bes.dto.v2.manualreceipt.ManualReceiptBookResponse;
import za.co.mawa.bes.service.v2.ManualReceiptBookService;

import java.security.Principal;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/v2/manual-receipt-books")
@RequiredArgsConstructor
public class ManualReceiptBookControllerV2 {

    private final ManualReceiptBookService service;

    @GetMapping
    public List<ManualReceiptBookResponse> list(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.list(activeOnly);
    }

    @GetMapping("/{id}")
    public ManualReceiptBookResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ManualReceiptBookResponse> create(
            @RequestBody ManualReceiptBookRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor(principal)));
    }

    @PutMapping("/{id}")
    public ManualReceiptBookResponse update(
            @PathVariable String id,
            @RequestBody ManualReceiptBookRequest request,
            Principal principal) {
        return service.update(id, request, actor(principal));
    }

    @DeleteMapping("/{id}")
    public ManualReceiptBookResponse deactivate(@PathVariable String id, Principal principal) {
        return service.deactivate(id, actor(principal));
    }

    private String actor(Principal principal) {
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        return "SYSTEM";
    }
}
