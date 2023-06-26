package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import za.co.mawa.bes.utils.IdType;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Validate;

@RestController
@CrossOrigin
public class ValidateController {
    Gson gson = new Gson();

    @RequestMapping(value= "/validate/identity/{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateID(@PathVariable String id,@RequestParam String type){
       try{
           boolean valid = false;
           if(type.equalsIgnoreCase(IdType.SA_ID)) {
               if(id.length() == Constant.SA_ID_LENGTH && Validate.checkDigit(id)){
                   valid = true;
               }
           }
           return ResponseEntity.ok(gson.toJson(valid));
       }catch (Exception ex){
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
       }
    }
}
