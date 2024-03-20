package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantCreateDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestCreateDto;
import za.co.mawa.bes.object.tenant.TenantDTO;
import za.co.mawa.bes.service.TenantService;

@RestController
@CrossOrigin
@RequestMapping(value = "system-configuration")
public class SystemConfigurationController {
    Gson gson = new Gson();
    @Autowired
    TenantService tenantService;
    @RequestMapping(value = "/tenant-property", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTenantRequest() {
        try{
            String response = gson.toJson(tenantService.getTenantProperties(TenantContext.getCurrentTenant()));
            return ResponseEntity.ok(response);
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }

    }
    @RequestMapping(value = "/tenant", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postTenantDetailsRequest(@RequestBody TenantCreateDto tenantCreate) {
        try{
            String response = gson.toJson(tenantService.addTenantPartner(tenantCreate,TenantContext.getCurrentTenant()));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

}
