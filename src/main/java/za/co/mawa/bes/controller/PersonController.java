package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.IdentityDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.PersonService;
import za.co.mawa.bes.dto.PartnerQueryDto;
import za.co.mawa.bes.utils.PartnerType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class PersonController {
    @Autowired
    PersonService personService;
    @Autowired
    PartnerService partnerService;
    Gson gson = new Gson();

    @RequestMapping(value = "/persons", method = RequestMethod.POST)
    public ResponseEntity<?> createPerson(@RequestBody PersonDto person) throws Exception {
        try {
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
        String response = null;
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


            }
        } else {
//            ArrayList<PartnerDto> initialList = new ArrayList<>();
//            ArrayList<PartnerDto> objects = partnerService.search(null);
//            for (PartnerDto partnerDto : objects) {
//             if (partnerDto.getType() != null)
//            {
//
//                if (partnerDto.getType().equals(PartnerType.PERSON) ||
//                        partnerDto.getType().equals(PartnerType.INDIVIDUAL)) {
//                    initialList.add(partnerDto);
//                }
//            }
//
//            }
//
//            for (PartnerDto object : initialList) {
//                PersonDto person = new PersonDto(object);
//                persons.add(person);
//            }

            List<PartnerDto> objects = partnerService.search(null);

            List<PartnerDto> filteredList = objects.stream()
                    .filter(partnerDto -> partnerDto.getType() != null &&
                            (partnerDto.getType().equals(PartnerType.PERSON) ||
                                    partnerDto.getType().equals(PartnerType.INDIVIDUAL)))
                    .collect(Collectors.toList());

            List<PersonDto> personDtoList = filteredList.stream()
                    .map(PersonDto::new)
                    .collect(Collectors.toList());
            response = gson.toJson(personDtoList);
        }

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/persons/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPerson(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok(personService.getPerson(id));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/persons/{id}/identity", method = RequestMethod.POST)
    public ResponseEntity<?> addIdentity(@PathVariable String id, @RequestBody IdentityDto identity) throws Exception {
        try {
            identity.setPartner(id);
            Boolean Identity = partnerService.addIdentity(identity);
            return ResponseEntity.ok(gson.toJson(Identity));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/persons/{id}/identity", method = RequestMethod.GET)
    public ResponseEntity<?> getIdentity(@PathVariable String id) throws Exception {
        try {
            return ResponseEntity.ok(partnerService.getIdentities(id));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/persons/{id}/identity", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteIdentity(@PathVariable String id,
                                            @Param("idType") String type,
                                            @Param("idNumber") String idValue) throws Exception {
        try {
            IdentityDto identity = new IdentityDto();
            identity.setPartner(id);
            if (type != null) {
                identity.setIdType(type);
            }
            if (idValue != null) {
                identity.setIdNumber(idValue);
            }
            boolean Deleted = partnerService.removeIdentity(identity);
            return ResponseEntity.ok(Deleted);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
