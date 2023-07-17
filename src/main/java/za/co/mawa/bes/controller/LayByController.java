package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.layby.LayByCreateDto;
import za.co.mawa.bes.dto.layby.LayByDto;
import za.co.mawa.bes.dto.layby.LayByEditDto;
import za.co.mawa.bes.dto.layby.LayByQueryDto;
import za.co.mawa.bes.service.LayByService;

@RestController
@CrossOrigin
public class LayByController {
    Gson gson = new Gson();
    @Autowired
    LayByService layByService;
    @RequestMapping(value= "/layBy" , method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createLayBy(@RequestBody LayByCreateDto layByCreateDto){
        try{
            String id = layByService.create(layByCreateDto);
            LayByDto lay = new LayByDto();
            lay.setId(id);
            return ResponseEntity.ok(gson.toJson(lay));
        }catch (Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value= "/layBy/{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getLayBy(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(layByService.get(id)));
        }catch (Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value= "/layBy/{id}" , method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> editLayBy(@PathVariable String id, @RequestBody LayByEditDto edit){
        try{
            return ResponseEntity.ok(gson.toJson(layByService.edit(id,edit)));
        }catch (Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
//    @RequestMapping(value= "/layBy/{id}" , method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
//    ResponseEntity<?> deleteLayBy(@PathVariable String id){
//        try{
//            return ResponseEntity.ok(gson.toJson(layByService.delete(id)));
//        }catch (Exception exception){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
//        }
//    }

    @RequestMapping(value= "/layBy" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getLayBys(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String customerId,
                                @RequestParam(required = false) String salesRepresentativeId,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String dateCreated,
                                @RequestParam(required = false) String number){
        try{
            LayByQueryDto queryDto = new LayByQueryDto();
            if(status != null && status != ""){
               queryDto.setStatus(status);
            }
            if(customerId != null && customerId != ""){
                queryDto.setCustomerId(customerId);
            }
            if(salesRepresentativeId != null && salesRepresentativeId != ""){
               queryDto.setSalesRepresentativeId(salesRepresentativeId);
            }
            if(endDate != null && endDate != ""){
               queryDto.setEndDate(endDate);
            }
            if(dateCreated != null && dateCreated != ""){
                queryDto.setDateCreated(dateCreated);
            }
            if(number != null && number != ""){
                queryDto.setNumber(number);
            }
            return ResponseEntity.ok(gson.toJson(layByService.search(queryDto)));
        }catch (Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
