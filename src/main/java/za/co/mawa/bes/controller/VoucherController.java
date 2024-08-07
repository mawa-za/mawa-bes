package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.voucher.*;
import za.co.mawa.bes.service.VoucherService;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "voucher")
public class VoucherController {
    Gson gson = new Gson();
    @Autowired
    VoucherService voucherService;

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createVoucher(@RequestBody VoucherInboundDto voucherInboundDto){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.create(voucherInboundDto)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getVoucher(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.get(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getVouchers(@RequestParam(required = false) String parent) {
        try {
            VoucherQuery query = new VoucherQuery();
            if (parent != null && !parent.isEmpty()) {
                query.setStatus(parent);
            }
            List<VoucherOutboundDto> vouchers = voucherService.search(query);
            return ResponseEntity.ok(vouchers);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }
    @RequestMapping(value= "{id}" , method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editVoucher(@PathVariable String id, @RequestBody VoucherEditDto voucherEditDto){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.edit(id,voucherEditDto)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "{id}" , method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteVoucher(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(voucherService.delete(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
