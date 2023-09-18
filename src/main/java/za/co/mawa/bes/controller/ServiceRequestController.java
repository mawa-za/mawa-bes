package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestEditDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.service.ServiceRequestService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
public class ServiceRequestController {
    @Autowired
    ServiceRequestService serviceRequestService;
    Gson gson = new Gson();

    @RequestMapping(value = "/service-request", method = RequestMethod.POST)
    public ResponseEntity<?> postServiceRequest(@RequestBody ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.create(serviceRequestCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/service-request", method = RequestMethod.GET)
    public ResponseEntity<?> getServiceRequest() {
        try {
            ServiceRequestQueryDto serviceRequestQueryDto = new ServiceRequestQueryDto();
            return ResponseEntity.ok(gson.toJson(serviceRequestService.search(serviceRequestQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getServiceRequest(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editServiceRequest(@PathVariable String id, @RequestBody ServiceRequestEditDto serviceRequestEditDto) {
        try {
            serviceRequestEditDto.setId(id);
            serviceRequestService.edit(serviceRequestEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteServiceRequest(@PathVariable String id) {
        try {
            serviceRequestService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
