package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.PersonService;
import za.co.mawa.bes.utils.RoleType;
import za.co.mawa.bes.dto.PartnerQueryDto;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class CustomerController {
    @Autowired
    PartnerService partnerService;
    @Autowired
    PersonService personService;
    Gson gson = new Gson();

    @RequestMapping(value = "/customer", method = RequestMethod.GET)
    public ResponseEntity<?> getCustomers() throws Exception {
        PartnerQueryDto query = new PartnerQueryDto();
        query.setRole(RoleType.CUSTOMER);

        ArrayList<PartnerDto> objects = partnerService.search(query);
        ArrayList<PersonDto> persons = new ArrayList<>();
        for (PartnerDto object : objects) {
            PersonDto person = new PersonDto(object);
            persons.add(person);
        }
        String response = gson.toJson(persons);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/customer", method = RequestMethod.POST)
    public ResponseEntity<?> postCustomer(@RequestBody PersonDto person) throws Exception {
        try {
            String personDto = personService.createPerson(person);
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/customer", method = RequestMethod.PUT)
    public String putCustomer() throws Exception {
        return null;
    }

    @RequestMapping(value = "/customer/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getCustomer(@PathVariable String id) throws Exception {
        try {
            List<String> roles = partnerService.getRoles(id);
            PersonDto personDto = roles.stream()
                    .filter(role -> role.equals(RoleType.CUSTOMER))
                    .findFirst()
                    .map(role -> {
                        PartnerDto partnerDto = null;
                        try {
                            partnerDto = partnerService.get(id);
                            if (partnerDto.getId() != null) {
                                return new PersonDto(partnerDto);
                            }
                        } catch (PartnerNotFound e) {
                            throw new RuntimeException(e);
                        }

                        return null;
                    })
                    .orElse(new PersonDto());

            String response = gson.toJson(personDto);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
