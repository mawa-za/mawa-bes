package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.service.EmploymentService;

import java.util.List;

@RestController
@CrossOrigin
public class EmployeeController {
    Gson gson = new Gson();
    @Autowired
    EmploymentService employmentService;

    @RequestMapping(value = "/Employees", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployees(@RequestParam(required = false) String contacts,
                                          @RequestParam(required = false) String identities,
                                          @RequestParam(required = false) String addresses) {


      EmploymentDto employmentDto = new EmploymentDto();

        try {

            if( contacts != null  )
            {

            }
            return ResponseEntity.ok(gson.toJson(employmentService.getAll()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @RequestMapping(value = "/Employee{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployee(@PathVariable String id) {

        try {
            return ResponseEntity.ok(gson.toJson(employmentService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @RequestMapping(value = "/Employee{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editEmployee(@PathVariable String id, @RequestBody EmploymentDto employmentDto) {
        try {

            employmentDto.setEmployeeId(id);
            boolean edited = employmentService.edit(employmentDto);

            return ResponseEntity.ok(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }



}
