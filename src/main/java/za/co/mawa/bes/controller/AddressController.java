package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.AddressCreateDto;
import za.co.mawa.bes.dto.AddressEditDto;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.service.AddressService;
import za.co.mawa.bes.service.BankAccountService;

@RestController
@CrossOrigin
@RequestMapping(value = "address")
public class AddressController {
    Gson gson = new Gson();
    @Autowired
    AddressService addressService;
    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAddress(@RequestBody AddressCreateDto addressCreateDto) {
        try {
            addressService.add(addressCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAddresses(@RequestParam(required = true) String objectId) {
        try {
            return ResponseEntity.ok(gson.toJson(addressService.getList(objectId)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<?> getAddress(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(addressService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editAddress(@PathVariable String id, @RequestBody AddressEditDto addressEditDto) {
        try {
            addressService.edit(addressEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAddress(@PathVariable String id) {
        try {
            addressService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
