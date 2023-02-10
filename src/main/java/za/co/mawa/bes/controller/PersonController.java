package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.service.PersonService;
import za.co.mawa.bes.service.UserService;
@RestController
@CrossOrigin
public class PersonController {
    @Autowired
    PersonService personService;
    Gson gson = new Gson();

    @RequestMapping(value = "/persons", method = RequestMethod.POST)
    public ResponseEntity<?> creatPerson (@RequestBody PersonDto person) {
        try{
            return ResponseEntity.ok(personService.createPerson(person));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
