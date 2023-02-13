package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.PersonService;
import za.co.mawa.bes.service.UserService;
import za.co.raretag.mawabes.dto.PartnerQueryDto;

import java.util.ArrayList;

@RestController
@CrossOrigin
public class PersonController {
    @Autowired
    PersonService personService;
    @Autowired
    PartnerService partnerService;
    Gson gson = new Gson();

    @RequestMapping(value = "/persons", method = RequestMethod.POST)
    public ResponseEntity<?> createPerson (@RequestBody PersonDto person) throws Exception{
        try{
            String personDto = personService.createPerson(person);
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/persons", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@Param("idnumber") String idnumber,
                                      @Param("idtype") String idtype,
                                      @Param("surname") String surname,
                                      @Param("name") String name,
                                      @Param("cellnumber") String cellnumber,
                                      @Param("filter") String filter) throws Exception {
        ArrayList<PersonDto> persons = new ArrayList<>();
        String response;
        if (filter != null) {
            if ("X".equals(filter.toUpperCase())) {
               PartnerQueryDto query = new PartnerQueryDto();
                query.setIdType(idtype);
                query.setIdNumber(idnumber);
                query.setName1(surname);
                query.setName2(name);
                query.setName3(name);
                query.setCellphone(cellnumber);
                ArrayList<PartnerDto> objects = partnerService.search(query);
                for (PartnerDto object : objects) {
                    PersonDto person = new PersonDto(object);
                    persons.add(person);
                }
                response = gson.toJson(persons);
            } else {
                ArrayList<PartnerDto> objects = partnerService.search(null);
                for (PartnerDto object : objects) {
                    PersonDto person = new PersonDto(object);
                    persons.add(person);
                }
                response = gson.toJson(persons);
            }
        } else {
            ArrayList<PartnerDto> objects = partnerService.search(null);
            for (PartnerDto object : objects) {
                PersonDto person = new PersonDto(object);
                persons.add(person);
            }
            response = gson.toJson(persons);
        }
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/persons/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        try{
            return ResponseEntity.ok(personService.getPerson(id));
        }catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
