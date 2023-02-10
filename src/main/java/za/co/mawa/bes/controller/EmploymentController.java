package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.service.EmploymentService;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class EmploymentController {
    Gson gson = new Gson();
    @Autowired
    EmploymentService employmentService;

    @RequestMapping(value = "/employment", method = RequestMethod.GET)
    public ResponseEntity<?> getEmployees(@Param("orgID") String orgID, @Param("approver") String approver) throws Exception{
        String response = null;
        List<EmploymentDto> employees = new ArrayList<>();
        if(orgID != null && approver == null){
             employees = employmentService.getByOrg(orgID);
            response = gson.toJson(employees);
        }
        if (approver != null && orgID == null) {
             employees = employmentService.getByApprover(approver);
            response = gson.toJson(employees);
        }

        return ResponseEntity.ok(gson.toJson(employees));
    }

    @RequestMapping(value = "/employment/terminate", method = RequestMethod.GET)
    public ResponseEntity<?> terminate(@Param("approver") String approver) throws Exception{
        String response = null;
        List<EmploymentDto> employees = employmentService.getByApprover(approver);
        return ResponseEntity.ok(gson.toJson(employees));
    }

    @RequestMapping(value = "/employment", method = RequestMethod.POST)
    public  ResponseEntity<?> createEmployee(@RequestBody EmploymentDto employmentDto){
        return ResponseEntity.ok(employmentService.hire(employmentDto));
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getEmployee(@PathVariable String id) throws Exception{
        return ResponseEntity.ok(employmentService.get(id));
    }

    @RequestMapping(value = "/employment/{id}", method = RequestMethod.PUT)
    public  ResponseEntity<?> editEmployee(@PathVariable String id,@RequestBody EmploymentDto employmentDto){
        employmentDto.setEmployeeId(id);
        return ResponseEntity.ok(employmentService.edit(employmentDto));
    }

    @RequestMapping(value = "/employment/{id}/terminate", method = RequestMethod.PUT)
    public  ResponseEntity<?> terminate(@PathVariable String id,@RequestBody EmploymentDto employmentDto){
        employmentDto.setEmployeeId(id);
        return ResponseEntity.ok(employmentService.terminate(employmentDto));
    }

    @RequestMapping(value = "/employment/{id}/suspend", method = RequestMethod.PUT)
    public  ResponseEntity<?> suspend(@PathVariable String id,@RequestBody EmploymentDto employmentDto){
        employmentDto.setEmployeeId(id);
        return ResponseEntity.ok(employmentService.suspend(employmentDto));
    }

    @RequestMapping(value = "/employment/{id}/rehire", method = RequestMethod.PUT)
    public  ResponseEntity<?> rehire(@PathVariable String id,@RequestBody EmploymentDto employmentDto){
        employmentDto.setEmployeeId(id);
        return ResponseEntity.ok(employmentService.rehire(employmentDto));
    }

}
