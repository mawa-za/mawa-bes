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
public class InquiryController {
    Gson gson = new Gson();
    @Autowired
    ServiceRequestService serviceRequestService;
    @RequestMapping(value = "/inquiry", method = RequestMethod.POST)
    public ResponseEntity<?> postInquiry() {
        return null;
    }
    @RequestMapping(value = "/inquiry", method = RequestMethod.GET)
    public ResponseEntity<?> getInquiries() {
        return null;
    }
    @RequestMapping(value = "/inquiry/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getInquiry() {
        return null;
    }
    @RequestMapping(value = "/inquiry/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editInquiry() {
        return null;
    }
    @RequestMapping(value = "/inquiry/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteInquiry() {
        return null;
    }

}
