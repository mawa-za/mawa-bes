package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.AssetHireService;

import java.time.LocalDateTime;

@RestController
@CrossOrigin
@RequestMapping("/v2/product-hire")
public class AssetHireControllerV2 {

    private final AssetHireService service;

    public AssetHireControllerV2(AssetHireService service) {
        this.service = service;
    }

    @GetMapping("/services/{productId}/assets")
    public ResponseEntity<?> getLinkedAssets(@PathVariable String productId,
                                             @RequestParam(required = false) LocalDateTime startAt,
                                             @RequestParam(required = false) LocalDateTime endAt) {
        try {
            return ResponseEntity.ok(service.getLinkedAssets(productId, startAt, endAt));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping("/services/{productId}/assets")
    public ResponseEntity<?> replaceLinkedAssets(@PathVariable String productId,
                                                 @RequestBody AssetHireService.AssetLinkSetRequest request,
                                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.ok(service.replaceLinkedAssets(productId, request, userId));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<?> listReservations(@RequestParam(required = false) String query,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) LocalDateTime from,
                                              @RequestParam(required = false) LocalDateTime to) {
        try {
            return ResponseEntity.ok(service.listReservations(query, status, from, to));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(@RequestBody AssetHireService.ReservationRequest request,
                                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createReservation(request, userId));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping("/reservations/{id}/issue")
    public ResponseEntity<?> issue(@PathVariable String id,
                                   @RequestBody(required = false) AssetHireService.ConditionRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.ok(service.issue(id, request, userId));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping("/reservations/{id}/return")
    public ResponseEntity<?> returnAsset(@PathVariable String id,
                                         @RequestBody AssetHireService.ReturnRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.ok(service.returnAsset(id, request, userId));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id,
                                    @RequestBody(required = false) AssetHireService.CancelRequest request,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.ok(service.cancel(id, request == null ? null : request.notes(), userId));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
