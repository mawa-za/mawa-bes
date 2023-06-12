package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.dto.prospect.ProspectCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.service.SupplierService;
import za.co.mawa.bes.service.UserService;
import za.co.mawa.bes.utils.PartnerType;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
public class SupplierController {

    Gson gson = new Gson();
    @Autowired
    UserService userService;
    @Autowired
    SupplierService supplierService;

    @RequestMapping(value = "/supplier/assign", method = RequestMethod.PUT)
    public ResponseEntity<?> postSupplierRequest(@RequestParam(required = false) String username, @RequestParam(required = false) String partnerId) {
        try {

            SupplierDto supplierDto = new SupplierDto();
            if (username != null) {
                supplierDto.setUsername(username);
            }
            if (partnerId != null) {
                supplierDto.setPartnerId(partnerId);
            }

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
}
