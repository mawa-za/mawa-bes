package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.servicemanagement.ServiceManagementDtos;
import za.co.mawa.bes.service.v2.ServiceManagementService;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/service-management")
public class ServiceManagementControllerV2 {
    private final ServiceManagementService service;

    public ServiceManagementControllerV2(ServiceManagementService service) { this.service = service; }

    @GetMapping("/dashboard") public ResponseEntity<?> dashboard(){return execute(service::dashboard,HttpStatus.OK);}

    @GetMapping("/locations") public ResponseEntity<?> locations(@RequestParam(required=false) String customerPartnerId){
        return execute(() -> service.locations(customerPartnerId), HttpStatus.OK);
    }
    @PutMapping("/locations") public ResponseEntity<?> saveLocation(@RequestBody ServiceManagementDtos.LocationRequest request){
        return execute(() -> service.saveLocation(request), HttpStatus.OK);
    }

    @GetMapping("/resources") public ResponseEntity<?> resources(){return execute(service::resources,HttpStatus.OK);}
    @PutMapping("/resources") public ResponseEntity<?> saveResource(@RequestBody ServiceManagementDtos.ResourceRequest request){
        return execute(() -> service.saveResource(request), HttpStatus.OK);
    }

    @GetMapping("/resource-requirements") public ResponseEntity<?> requirements(@RequestParam String productId){
        return execute(() -> service.requirements(productId), HttpStatus.OK);
    }
    @PutMapping("/resource-requirements") public ResponseEntity<?> saveRequirement(@RequestBody ServiceManagementDtos.ResourceRequirementRequest request){
        return execute(() -> service.saveRequirement(request), HttpStatus.OK);
    }
    @DeleteMapping("/resource-requirements/{id}") public ResponseEntity<?> deleteRequirement(@PathVariable String id){
        return execute(() -> { service.deleteRequirement(id); return Map.of("success",true); }, HttpStatus.OK);
    }

    @GetMapping("/contracts") public ResponseEntity<?> contracts(@RequestParam(required=false) String status,@RequestParam(required=false) String customerPartnerId){
        return execute(() -> service.contracts(status, customerPartnerId), HttpStatus.OK);
    }
    @GetMapping("/contracts/{id}") public ResponseEntity<?> contract(@PathVariable String id){return execute(() -> service.contract(id),HttpStatus.OK);}
    @PutMapping("/contracts") public ResponseEntity<?> saveContract(@RequestBody ServiceManagementDtos.ContractRequest request){
        return execute(() -> service.saveContract(request), HttpStatus.OK);
    }
    @PostMapping("/contracts/{id}/status/{status}") public ResponseEntity<?> contractStatus(@PathVariable String id,@PathVariable String status){
        return execute(() -> service.changeContractStatus(id,status),HttpStatus.OK);
    }

    @PostMapping("/availability") public ResponseEntity<?> availability(@RequestBody ServiceManagementDtos.AvailabilityRequest request){
        return execute(() -> service.availability(request),HttpStatus.OK);
    }

    @GetMapping("/requests") public ResponseEntity<?> requests(@RequestParam(required=false) String status){return execute(() -> service.requests(status),HttpStatus.OK);}
    @GetMapping("/requests/{id}/metadata") public ResponseEntity<?> requestMetadata(@PathVariable String id){return execute(() -> service.requestMetadata(id),HttpStatus.OK);}
    @PostMapping("/requests/{id}/order") public ResponseEntity<?> createOrder(@PathVariable String id){return execute(() -> service.createOrderFromRequest(id),HttpStatus.OK);}
    @PostMapping("/requests/{id}/contract") public ResponseEntity<?> createContract(@PathVariable String id){return execute(() -> service.createContractFromRequest(id),HttpStatus.OK);}
    @PutMapping("/requests/metadata") public ResponseEntity<?> saveRequestMetadata(@RequestBody ServiceManagementDtos.RequestMetadataRequest request){
        return execute(() -> service.saveRequestMetadata(request),HttpStatus.OK);
    }

    @PostMapping("/recurring/generate") public ResponseEntity<?> generateRecurring(){return execute(service::generateRecurring,HttpStatus.OK);}

    private ResponseEntity<?> execute(Action action,HttpStatus status){
        try{return ResponseEntity.status(status).body(action.run());}
        catch(Exception ex){Map<String,Object> body=new LinkedHashMap<>();body.put("message",ex.getMessage()==null?ex.toString():ex.getMessage());return ResponseEntity.badRequest().body(body);}
    }
    @FunctionalInterface private interface Action{Object run() throws Exception;}
}
