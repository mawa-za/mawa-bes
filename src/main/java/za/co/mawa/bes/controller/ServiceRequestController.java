package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.TransactionCreateDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.service.SalesOrderService;
import za.co.mawa.bes.service.ServiceRequestService;
@RestController
@CrossOrigin
public class ServiceRequestController {
    @Autowired
    ServiceRequestService serviceRequestService;
    Gson gson = new Gson();

    @RequestMapping(value = "/service-request", method = RequestMethod.POST)
    public ResponseEntity<?> postServiceRequest(@RequestBody TransactionCreateDto transactionCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/service-request", method = RequestMethod.GET)
    public ResponseEntity<?> getServiceRequest(@RequestBody TransactionQueryDto transactionQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(serviceRequestService.search(transactionQueryDto)));
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
    public ResponseEntity<?> editServiceRequest(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            serviceRequestService.edit(transactionDto);
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
