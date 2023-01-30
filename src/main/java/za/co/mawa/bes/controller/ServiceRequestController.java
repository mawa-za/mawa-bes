package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.service.ServiceRequestService;
@RestController
@CrossOrigin
public class ServiceRequestController {
    Gson gson = new Gson();
    @Autowired
    ServiceRequestService serviceRequestService;

    public ResponseEntity<?> create() {
        return null;
    }

}
