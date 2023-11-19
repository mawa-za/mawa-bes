package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.service.SupplierService;
import za.co.mawa.bes.service.UserService;

@RestController
@CrossOrigin
public class SupplierController {

    Gson gson = new Gson();
    @Autowired
    UserService userService;
    @Autowired
    SupplierService supplierService;

    @RequestMapping(value = "/supplier/assign", method = RequestMethod.POST)
    public ResponseEntity<?> postSupplierRequest(@RequestParam(required = false) String username) {
        try {

            SupplierDto supplierDto = new SupplierDto();
            if (username != null) {
                supplierDto.setUsername(username);
            }
//            if (partnerId != null) {
//                supplierDto.setPartnerId(partnerId);
//            }

            return ResponseEntity.ok(gson.toJson(supplierService.assignSupplier(supplierDto)));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/supplier/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSupplierRequest(@PathVariable String id) {
        try {

            return ResponseEntity.ok(gson.toJson(supplierService.getSupplier(id)));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/supplier", method = RequestMethod.GET)
    public ResponseEntity<?> getSuppliers() {
        try {

            return ResponseEntity.ok(gson.toJson(supplierService.getAll()));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
