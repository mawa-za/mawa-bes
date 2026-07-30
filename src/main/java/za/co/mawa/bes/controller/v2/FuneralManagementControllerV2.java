package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.funeral.*;
import za.co.mawa.bes.dto.v2.FuneralPackageCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPackageUpdateRequestDto;
import za.co.mawa.bes.service.v2.FuneralManagementService;

@RestController
@CrossOrigin
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/v2/funeral", produces = MediaType.APPLICATION_JSON_VALUE)
public class FuneralManagementControllerV2 {

    private final FuneralManagementService funeralManagementService;

    @GetMapping("/pickup-requests")
    public ResponseEntity<?> getPickupRequests() {
        try {
            return ResponseEntity.ok(funeralManagementService.getPickupRequests());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

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

    @PutMapping(value = "/pickup-request/{id}/arrive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> arriveAtPickupLocation(@PathVariable String id, @RequestBody ArrivePickupRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.arriveAtPickupLocation(id, request));
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
    public ResponseEntity<?> getPackages(@RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            return ResponseEntity.ok(funeralManagementService.getPackages(activeOnly));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<?> getPackage(@PathVariable String id) {
        try {
            return ResponseEntity.ok(funeralManagementService.getPackage(id));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/packages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPackage(@RequestBody FuneralPackageCreateRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.createPackage(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/packages/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updatePackage(@PathVariable String id, @RequestBody FuneralPackageUpdateRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.updatePackage(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<?> deletePackage(@PathVariable String id) {
        try {
            funeralManagementService.deletePackage(id);
            return ResponseEntity.noContent().build();
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

    @GetMapping("/service-requests")
    public ResponseEntity<?> getServiceRequests(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(funeralManagementService.getServiceRequests(query, status));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }


    @GetMapping("/payments")
    public ResponseEntity<?> getFuneralPayments() {
        try {
            return ResponseEntity.ok(funeralManagementService.getFuneralPayments());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }


    @GetMapping("/configuration")
    public ResponseEntity<?> getConfiguration() {
        try {
            return ResponseEntity.ok(funeralManagementService.getServiceConfiguration());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/tenant-integration")
    public ResponseEntity<?> getTenantIntegrationConfiguration() {
        try {
            return ResponseEntity.ok(funeralManagementService.getTenantIntegrationConfiguration());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/tenant-integration", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateTenantIntegrationConfiguration(
            @RequestBody FuneralTenantIntegrationConfigDto request
    ) {
        try {
            return ResponseEntity.ok(funeralManagementService.updateTenantIntegrationConfiguration(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/tenant-integration/available-tenants")
    public ResponseEntity<?> getAvailableTenantOptions() {
        try {
            return ResponseEntity.ok(funeralManagementService.getAvailableTenantOptions());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }


    @GetMapping("/trusted-tenants")
    public ResponseEntity<?> getTrustedTenants() {
        try { return ResponseEntity.ok(funeralManagementService.getTrustedTenantRelationships()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); }
    }

    @PostMapping(value = "/trusted-tenants", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requestTrustedTenant(@RequestBody TenantTrustRelationshipDto request) {
        try { return ResponseEntity.ok(funeralManagementService.requestTrustedTenantRelationship(request)); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); }
    }

    @PutMapping("/trusted-tenants/{id}/status/{status}")
    public ResponseEntity<?> updateTrustedTenantStatus(@PathVariable String id, @PathVariable String status) {
        try { return ResponseEntity.ok(funeralManagementService.updateTrustedTenantRelationshipStatus(id, status)); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); }
    }

    @PutMapping(value = "/configuration", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateConfiguration(@RequestBody FuneralServiceConfigurationDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.updateServiceConfiguration(request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/service-request/{id}")
    public ResponseEntity<?> getServiceRequest(@PathVariable String id) {
        try {
            return ResponseEntity.ok(funeralManagementService.getServiceRequest(id));
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


    @PutMapping(value = "/service-request/{id}/package", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateServiceRequestPackage(@PathVariable String id, @RequestBody FuneralServiceRequestDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.updateServiceRequestPackage(id, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/service-request/{id}/initiate-claims", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> initiateClaims(@PathVariable String id, @RequestBody InitiateFuneralClaimsDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(funeralManagementService.initiateClaims(id, request));
        } catch (Exception exception) {
            log.warn("Unable to initiate claims for funeral service {} with memberships {}: {}",
                    id,
                    request == null ? null : request.getMemberships(),
                    exception.getMessage(),
                    exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/service-request/{id}/claims")
    public ResponseEntity<?> getClaims(@PathVariable String id) {
        try {
            return ResponseEntity.ok(funeralManagementService.getClaims(id));
        } catch (Exception exception) {
            log.warn("Unable to load claims for funeral service {}: {}", id, exception.getMessage(), exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping(value = "/claims/{membershipClaimId}/claim-form", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadClaimForm(@PathVariable String membershipClaimId) {
        try {
            byte[] pdf = funeralManagementService.downloadClaimForm(membershipClaimId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=claim-form-" + membershipClaimId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(exception.getMessage());
        }
    }

    @PutMapping(value = "/service-request/{id}/wizard-step/{wizardStep}")
    public ResponseEntity<?> updateWizardStep(@PathVariable String id, @PathVariable int wizardStep) {
        try {
            return ResponseEntity.ok(funeralManagementService.updateWizardStep(id, wizardStep));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping(value = "/claims/{membershipClaimId}/submit-for-approval")
    public ResponseEntity<?> submitClaimForApproval(
            @PathVariable String membershipClaimId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        try {
            return ResponseEntity.ok(funeralManagementService.submitClaimForApproval(membershipClaimId, userId));
        } catch (Exception exception) {
            log.warn("Unable to submit funeral claim {} for approval: {}",
                    membershipClaimId, exception.getMessage(), exception);
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
            log.warn("Unable to generate invoices for funeral service {}: {}",
                    request == null ? null : request.getFuneralServiceId(),
                    exception.getMessage(),
                    exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
