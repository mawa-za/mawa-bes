package za.co.mawa.bes.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.deposit.DepositAttachmentCreateDto;
import za.co.mawa.bes.dto.deposit.DepositCreateDto;
import za.co.mawa.bes.dto.deposit.DepositDto;
import za.co.mawa.bes.dto.deposit.DepositSearchDto;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.DepositService;

@RestController
@CrossOrigin
@RequestMapping(value = "deposit")
public class DepositController {
    Gson gson = new Gson();
    @Autowired
    DepositService depositService;
    @Autowired
    AttachmentService attachmentService;

    @RequestMapping(  method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDeposit(@RequestBody DepositCreateDto deposit){
       try{
           DepositDto depositDto = new DepositDto();
           String id = depositService.create(deposit);
           depositDto.setId(id);
          return ResponseEntity.ok(gson.toJson(depositDto));
       }catch (Exception ex){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
       }
    }
    @RequestMapping(value= "deposit-attachment" , method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDepositAndAttachment(@RequestBody DepositAttachmentCreateDto depositAttachmentDto){
        try{

            DepositDto depositDto = new DepositDto();
            String id = depositService.createDepositAttachment(depositAttachmentDto);
            depositDto.setId(id);

            return ResponseEntity.ok(gson.toJson(depositDto));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDeposit(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(depositService.get(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value= "transactionLink" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByTransactionDeposit(@RequestParam(required = true) String transactionLinkId){
        try{
            return ResponseEntity.ok(gson.toJson(depositService.searchByTransactionLink(transactionLinkId)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping( method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllDeposits(@RequestParam(required = false) String createdBy,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String createdOn){
        try{
            DepositSearchDto searchDto = new DepositSearchDto();
            if(createdBy != null && createdBy != ""){
                searchDto.setCreatedBy(createdBy);
            }
            if(status != null && status != ""){
                searchDto.setStatus(status);
            }
            if(createdOn != null && createdOn != ""){
                searchDto.setCreatedOn(createdOn);
            }
            return ResponseEntity.ok(gson.toJson(depositService.search(searchDto)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}",method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteDeposit(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(depositService.delete(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
