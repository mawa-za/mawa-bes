package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.funeral.*;
import za.co.mawa.bes.service.v2.FuneralManagementService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping(value = "/v2/funeral", produces = MediaType.APPLICATION_JSON_VALUE)
public class FuneralManagementControllerV2 {

    private final FuneralManagementService funeralManagementService;

    @PostMapping(value = "/pickup-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPickupRequest(@RequestBody CreatePickupRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.createPickupRequest(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/pickup-request/{id}/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPickupRequest(@PathVariable String id, @RequestBody AssignPickupRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.assignPickupRequest(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/pickup-request/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completePickupRequest(@PathVariable String id, @RequestBody CompletePickupRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.completePickupRequest(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/mortuary/inventory")
    public ResponseEntity<?> getMortuaryInventory() {
        try {
            return ResponseEntity.ok(funeralManagementService.getMortuaryInventory());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/mortuary/{id}/checkout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkoutMortuary(@PathVariable String id, @RequestBody MortuaryCheckoutDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.checkoutMortuary(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/packages")
    public ResponseEntity<?> getPackages() {
        try {
            return ResponseEntity.ok(funeralManagementService.getPackages());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/check-membership/{identityNumber}")
    public ResponseEntity<?> checkMembership(@PathVariable String identityNumber) {
        try {
            return ResponseEntity.ok(funeralManagementService.checkMembership(identityNumber));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/service-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createServiceRequest(@RequestBody FuneralServiceRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.createServiceRequest(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/service-request/{id}/initiate-claims", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> initiateClaims(@PathVariable String id, @RequestBody InitiateFuneralClaimsDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.initiateClaims(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/service-request/{id}/claims")
    public ResponseEntity<?> getClaims(@PathVariable String id) {
        try {
            return ResponseEntity.ok(funeralManagementService.getClaims(id));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/claims/{membershipClaimId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveClaim(@PathVariable String membershipClaimId, @RequestBody ApproveFuneralClaimDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.decideClaim(membershipClaimId, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/invoice-preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> previewInvoice(@RequestBody FuneralInvoicePreviewRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.previewInvoiceSplit(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/generate-invoices", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateInvoices(@RequestBody FuneralInvoicePreviewRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.generateInvoices(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
