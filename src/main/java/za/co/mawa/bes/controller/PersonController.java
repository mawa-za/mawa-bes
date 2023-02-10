package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> createPerson (@RequestBody PersonDto person) {
        try{
            String personDto = personService.createPerson(person);
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
