package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.service.SalesOrderService;
import za.co.mawa.bes.service.ServiceRequestService;

@RestController
@CrossOrigin
public class SalesOrderController {
    Gson gson = new Gson();
    @Autowired
    SalesOrderService salesOrderService;
    @RequestMapping(value = "/sales-order", method = RequestMethod.POST)
    public ResponseEntity<?> postSalesOrder() {
        return null;
    }
    @RequestMapping(value = "/sales-order", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrders() {
        return null;
    }
    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder() {
        return null;
    }
    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editSalesOrder() {
        return null;
    }
    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteSalesOrder() {
        return null;
    }
}
