package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmploymentCreateDto;
import za.co.mawa.bes.dto.EmploymentDto;
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
    public ResponseEntity<?> createEmployee(@RequestBody EmploymentCreateDto employmentDto){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Hiring requires approval. Use the v2 employment hire request endpoint"));
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
                                          @RequestParam(required = false) String partnerId,
                                          @RequestParam(required = false) String employeeNumber){
        try{
            EmploymentSearchDto search = new EmploymentSearchDto();
            if(startDate != null && !startDate.isBlank()){
                search.setStartDate(Conversion.stringToDate(startDate));
            }
            if(endDate != null && !endDate.isBlank()){
                search.setEndDate(Conversion.stringToDate(endDate));
            }
            if(branch != null && !branch.isBlank()){
                search.setBranch(branch);
            }
            if(department != null && !department.isBlank()){
              search.setDepartment(department);
            }
            if(position != null && !position.isBlank()){
                search.setPosition(position);
            }
            if(type != null && !type.isBlank()){
                search.setType(type);
            }
            if(status != null && !status.isBlank()){
                search.setStatus(status);
            }
            if(partnerId != null && !partnerId.isBlank()){
                search.setPartnerId(partnerId);
            }
            if(employeeNumber != null && !employeeNumber.isBlank()){
                search.setEmployeeNumber(employeeNumber);
            }
            return ResponseEntity.ok(gson.toJson(employmentService.getAll(search)));
        }catch(Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editEmployee(@PathVariable String id, @RequestBody EmploymentEditDto employmentDto){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 employment endpoint. Employee number and employment dates are controlled by approved lifecycle actions"));
    }

    @RequestMapping(value = "/employment/{id}/terminate", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> terminate(@PathVariable String id){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", "Termination requires approval. Use the v2 employment termination request endpoint"));
    }

    @RequestMapping(value = "/employment/{id}/suspend", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> suspend(@PathVariable String id){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", "Suspension requires approval. Use the v2 employment suspension request endpoint"));
    }

    @RequestMapping(value = "/employment/{id}/rehire", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rehire(@PathVariable String id, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", "Rehire requires approval. Use the v2 employment rehire request endpoint"));
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable String id, @RequestParam(required = true) String partner){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Employment records cannot be deleted because employment history must be preserved"));
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
