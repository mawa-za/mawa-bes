package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.service.FieldOptionService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class FieldOptionController {
    @Autowired
    FieldOptionService fieldOptionService;
    Gson gson = new Gson();
    @RequestMapping(value = "/field", method = RequestMethod.GET)
    public ResponseEntity<?> getFields() throws Exception {
        List<FieldDto> fieldDtoList = new ArrayList<>();
        fieldDtoList.add(new FieldDto("TITLE","Title"));
        fieldDtoList.add(new FieldDto("GENDER","Gender"));
        fieldDtoList.add(new FieldDto("IDTYPE","ID Type"));
        fieldDtoList.add(new FieldDto("USERROLE","User Role"));
        return ResponseEntity.ok(gson.toJson(fieldDtoList));
    }

    @RequestMapping(value = "/field/{field}/option", method = RequestMethod.GET)
    public ResponseEntity<?> getFieldOptions(@PathVariable String field) throws Exception {
        return ResponseEntity.ok(gson.toJson(fieldOptionService.getFieldOptions(field)));
    }
}
