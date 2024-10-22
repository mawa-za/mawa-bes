package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.FieldCreateDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.service.FieldOptionService;

@RestController
@CrossOrigin
public class FieldOptionController {
    @Autowired
    FieldOptionService fieldOptionService;
    Gson gson = new Gson();

    @RequestMapping(value = "/field", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFields(){
        try {
            return ResponseEntity.ok(gson.toJson(fieldOptionService.getFields()));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/field/option", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllFields(){
        try {
            return ResponseEntity.ok(gson.toJson(fieldOptionService.getAllFieldOptions()));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFieldOptions(@PathVariable String field) {
        try{
            return ResponseEntity.ok(gson.toJson(fieldOptionService.getFieldOptions(field)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
//
    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addFieldOption(@RequestBody FieldOptionDto fieldOptionDto, @PathVariable String field) {
        try {
            fieldOptionDto.setField(field);
            fieldOptionService.create(fieldOptionDto);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/field", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addField(@RequestBody FieldCreateDto fieldDto) {
        try {
            return ResponseEntity.ok().body(gson.toJson(fieldOptionService.createField(fieldDto)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFieldOption(@PathVariable String field, @RequestParam("fieldOption") String fieldOption) {
        try {
            fieldOptionService.deleteFieldOption(field,fieldOption);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
