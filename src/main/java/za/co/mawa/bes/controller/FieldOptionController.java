package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.service.FieldOptionService;

@RestController
@CrossOrigin
public class FieldOptionController {
    @Autowired
    FieldOptionService fieldOptionService;
    Gson gson = new Gson();

    @RequestMapping(value = "/field", method = RequestMethod.GET)
    public ResponseEntity<?> getFields(){
        return ResponseEntity.ok(gson.toJson(fieldOptionService.getFields()));
    }

    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.GET)
    public ResponseEntity<?> getFieldOptions(@PathVariable String field) {
        return ResponseEntity.ok(gson.toJson(fieldOptionService.getFieldOptions(field)));
    }
//
//    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.POST)
//    public ResponseEntity<?> addFieldOption(@RequestBody FieldOptionDto fieldOptionDto, @PathVariable String field) {
//        try {
//            fieldOptionDto.setField(field);
//            fieldOptionService.create(fieldOptionDto);
//            return ResponseEntity.ok().build();
//        } catch (FieldDoesNotExist ex) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
}
