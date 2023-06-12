package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.service.EmploymentService;
import za.co.mawa.bes.service.PartnerService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class EmployeeController {
    Gson gson = new Gson();
    @Autowired
    EmploymentService employmentService;
    @Autowired
    PartnerService partnerService;

    @RequestMapping(value = "/employees", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployees() {


        EmploymentDto employmentDto = new EmploymentDto();

        try {


            return ResponseEntity.ok(gson.toJson(employmentService.getAll()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @RequestMapping(value = "/employee/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployee(@PathVariable String id, @RequestParam(required = false) boolean contacts,
                                         @RequestParam(required = false) boolean identities,
                                         @RequestParam(required = false) boolean addresses) {

        try {

            EmploymentDto employmentDto = employmentService.get(id);

            if (contacts) {

                ArrayList<ContactDto> contactDtos = partnerService.getContacts(id);
                if (employmentDto != null) {
                    if (!contactDtos.isEmpty()) {
                        employmentDto.setContactDtos(contactDtos);
                    }

                }
            }
            if (identities) {
                if (employmentDto != null) {
                    ArrayList<IdentityDto> identityDtos = partnerService.getIdentities(id);
                    if (!identityDtos.isEmpty()) {
                        employmentDto.setIdentityDtos(identityDtos);
                    }
                }
            }

            if (addresses) {
                if (employmentDto != null) {

                    ArrayList<AddressDto> addressDtos = partnerService.getAddresses(id);
                    if (!addressDtos.isEmpty()) {
                        employmentDto.setAddressDtos(addressDtos);
                    }

                }

            }
            return ResponseEntity.ok(gson.toJson(employmentDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @RequestMapping(value = "/employee/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editEmployee(@PathVariable String id, @RequestBody EmploymentDto employmentDto) {
        try {

            employmentDto.setEmployeeId(id);
            boolean edited = employmentService.edit(employmentDto);

            return ResponseEntity.ok(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/employee/{id}/role", method = RequestMethod.POST)
    public ResponseEntity<?> assignRole(@PathVariable String id, @RequestParam("role") String role) {
        try {

            boolean success  = partnerService.assignRole(role, id);

            return ResponseEntity.ok(gson.toJson(success));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


}
