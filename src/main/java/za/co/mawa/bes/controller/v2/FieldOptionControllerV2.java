package za.co.mawa.bes.controller.v2;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.FieldCreateDto;
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
    Gson gson = new Gson();

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
            return ResponseEntity.badRequest().build();
        }
    }
    @RequestMapping(value = "/field", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FieldDto> addField(@RequestBody FieldCreateDto fieldDto) {
        try {
            return ResponseEntity.ok().body(fieldOptionService.createField(fieldDto));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }
    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFieldOption(@PathVariable String field, @RequestParam("fieldOption") String fieldOption) {
        try {
            fieldOptionService.deleteFieldOption(field,fieldOption);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
