package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmploymentCreateDto;
import za.co.mawa.bes.dto.EmploymentEditDto;
import za.co.mawa.bes.dto.EmploymentSearchDto;
import za.co.mawa.bes.service.EmploymentService;
import za.co.mawa.bes.utils.Conversion;

@RestController
@CrossOrigin
public class EmploymentController {
    Gson gson = new Gson();
    @Autowired
    EmploymentService employmentService;

    @RequestMapping(value = "/employment", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> createEmployee(@RequestBody EmploymentCreateDto employmentDto){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.hire(employmentDto)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployee(@PathVariable String id){
        try{
           return ResponseEntity.ok(gson.toJson(employmentService.get(id)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/employment", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEmployees(@RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(required = false) String branch,
                                          @RequestParam(required = false) String department,
                                          @RequestParam(required = false) String position,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String employeeId,
                                          @RequestParam(required = false) String partner,
                                          @RequestParam(required = false) String number){
        try{
            EmploymentSearchDto search = new EmploymentSearchDto();
            if(startDate != null && startDate != ""){
                search.setStartDate(Conversion.stringToDate(startDate));
            }
            if(endDate != null && endDate != ""){
                search.setEndDate(Conversion.stringToDate(endDate));
            }
            if(branch != null & branch != ""){
                search.setBranch(branch);
            }
            if(department != null && department != ""){
              search.setDepartment(department);
            }
            if(position != null && position != ""){
                search.setPosition(position);
            }
            if(type != null && type != ""){
                search.setType(type);
            }
            if(status != null && status != ""){
                search.setStatus(status);
            }
            if(employeeId != null && employeeId != ""){
                search.setEmployeeId(employeeId);
            }
            if(partner != null && partner != ""){
                search.setPartner(partner);
            }
            if(number != null && number != ""){
                search.setNumber(number);
            }
            return ResponseEntity.ok(gson.toJson(employmentService.getAll(search)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> editEmployee(@PathVariable String id,
                                           @RequestParam(required = true) String partner,
                                           @RequestBody EmploymentEditDto employmentDto){
        try{
            return ResponseEntity.ok(employmentService.edit(employmentDto,id,partner));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }

    }

    @RequestMapping(value = "/employment/{id}/terminate", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> terminate(@PathVariable String id,@RequestParam(required = true) String partner){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.terminate(id,partner)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/employment/{id}/suspend", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> suspend(@PathVariable String id,@RequestParam(required = true) String partner){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.suspend(id,partner)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/employment/{id}/rehire", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> rehire(@PathVariable String id,
                                     @RequestParam(required = true) String partner,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.rehire(id,partner,startDate,endDate)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/employment/{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> delete(@PathVariable String id, @RequestParam(required = true) String partner){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.deleteEmployment(id,partner)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
  
    @RequestMapping(value = "/employees", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<?> getEmployees(){
        try{
            return ResponseEntity.ok(gson.toJson(employmentService.getEmployees()));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
