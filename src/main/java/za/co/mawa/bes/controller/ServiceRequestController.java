package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.service.ServiceRequestService;
@RestController
@CrossOrigin
public class ServiceRequestController {
    Gson gson = new Gson();
    @Autowired
    ServiceRequestService serviceRequestService;
    @RequestMapping(value = "/service-request", method = RequestMethod.POST)
    public ResponseEntity<?> postServiceRequest() {
        return null;
    }
    @RequestMapping(value = "/service-request", method = RequestMethod.GET)
    public ResponseEntity<?> getServiceRequests() {
        return null;
    }
    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getServiceRequest() {
        return null;
    }
    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editServiceRequest() {
        return null;
    }
    @RequestMapping(value = "/service-request/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteServiceRequest() {
        return null;
    }

}
