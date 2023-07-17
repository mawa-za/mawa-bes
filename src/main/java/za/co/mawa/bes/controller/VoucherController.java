package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.voucher.VoucherCreateDto;
import za.co.mawa.bes.dto.voucher.VoucherDto;
import za.co.mawa.bes.dto.voucher.VoucherEditDto;
import za.co.mawa.bes.dto.voucher.VoucherQuery;
import za.co.mawa.bes.service.VoucherService;

@RestController
@CrossOrigin
public class VoucherController {
    Gson gson = new Gson();
    @Autowired
    VoucherService voucherService;

    @RequestMapping(value= "/voucher" , method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createVoucher(@RequestBody VoucherCreateDto voucherCreate){
        try{
            VoucherDto voucher = new VoucherDto();
            voucher.setId(voucherService.create(voucherCreate));
           return ResponseEntity.ok(gson.toJson(voucher));
        }catch (Exception ex){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "/voucher/{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getVoucher(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.get(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value= "/voucher" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getVouchers(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String customerId,
                                         @RequestParam(required = false) String number,
                                         @RequestParam(required = false) String dateCreated,
                                         @RequestParam(required = false) String expiryDate,
                                         @RequestParam(required = false) String IdNumber){
        try{
            VoucherQuery query = new VoucherQuery();
            if(status != null && status != ""){
                query.setStatus(status);
            }
            if(customerId != null && customerId != ""){
                query.setCustomerId(customerId);
            }
            if(number != null && number != ""){
                query.setNumber(number);
            }
            if(dateCreated != null && dateCreated != ""){
                query.setDateCreated(dateCreated);
            }
            if(expiryDate != null && expiryDate != ""){
                query.setExpiryDate(expiryDate);
            }
            if(IdNumber != null && IdNumber != ""){
                query.setIdNumber(IdNumber);
            }
            return ResponseEntity.ok(gson.toJson(voucherService.search(query)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "/voucher/{id}" , method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editVoucher(@PathVariable String id, @RequestBody VoucherEditDto editDto){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.edit(editDto,id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "/voucher/{id}" , method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteVoucher(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.delete(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
