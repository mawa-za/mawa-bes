package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestEditDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionEditDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.service.LeaveRequestService;
import za.co.mawa.bes.service.ServiceRequestService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
@RequestMapping(value = "service-request")
public class ServiceRequestController {
    @Autowired
    ServiceRequestService serviceRequestService;
    @Autowired
     TransactionService transactionService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postServiceRequest(@RequestBody ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.create(serviceRequestCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getServiceRequests(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String customer,
                                                @RequestParam(required = false) String priority,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String dueDate,
                                                @RequestParam(required = false) String creationDate) {
        try {
            ServiceRequestQueryDto serviceRequestQueryDto = new ServiceRequestQueryDto();
            serviceRequestQueryDto.setStatus(status);
            serviceRequestQueryDto.setCustomer(customer);
            serviceRequestQueryDto.setCategory(category);
            serviceRequestQueryDto.setPriority(priority);
            return ResponseEntity.ok(gson.toJson(serviceRequestService.search(serviceRequestQueryDto)));
        }
        catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getServiceRequest(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editServiceRequest(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok().body(gson.toJson(serviceRequestService.edit(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteServiceRequest(@PathVariable String id) {
        try {
            Boolean deleted = serviceRequestService.delete(id);
            return ResponseEntity.ok(gson.toJson(deleted));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/assign", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assign(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.assign(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/unassign", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unaAssign(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.unassign(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reject(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.reject(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.cancel(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/close", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> close(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.close(id, serviceRequestEditDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
