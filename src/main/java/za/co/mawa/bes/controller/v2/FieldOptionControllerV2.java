package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.service.FieldOptionService;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "v2")
public class FieldOptionControllerV2 {
    @Autowired
    FieldOptionService fieldOptionService;

    @RequestMapping(value = "/field", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FieldDto>> getFields(){
        try {
            return ResponseEntity.ok(fieldOptionService.getFields());
        }catch (Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }
    @RequestMapping(value = "/field/option", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FieldOptionDto>> getAllFields(){
        try {
            return ResponseEntity.ok(fieldOptionService.getAllFieldOptions());
        }catch (Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FieldOptionDto>> getFieldOptions(@PathVariable String field) {
        try{
            return ResponseEntity.ok(fieldOptionService.getFieldOptions(field));
        }catch(Exception ex){
            return ResponseEntity.badRequest().build();
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
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateFieldOption(
            @PathVariable String field,
            @RequestParam("fieldOption") String fieldOption,
            @RequestBody FieldOptionDto request) {
        try {
            return ResponseEntity.ok(fieldOptionService.update(field, fieldOption, request));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFieldOption(@PathVariable String field, @RequestParam("fieldOption") String fieldOption) {
        try {
            fieldOptionService.deleteFieldOption(field,fieldOption);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
