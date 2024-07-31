package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.leave.request.LeaveRequestInboundDto;
import za.co.mawa.bes.dto.leave.request.LeaveRequestQueryDto;
import za.co.mawa.bes.service.LeaveRequestService;

@RestController
@CrossOrigin
@RequestMapping(value = "leave-request")
public class LeaveRequestController {
    Gson gson = new Gson();
    @Autowired
    LeaveRequestService leaveRequestService;
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestBody LeaveRequestInboundDto leaveRequestInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(leaveRequestService.create(leaveRequestInboundDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(leaveRequestService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@RequestParam(required = false) String status) {
        try {
            LeaveRequestQueryDto leaveRequestQueryDto = new LeaveRequestQueryDto();
            return ResponseEntity.ok(gson.toJson(leaveRequestService.search(leaveRequestQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> put(@RequestBody LeaveRequestInboundDto leaveRequestInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(leaveRequestService.create(leaveRequestInboundDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(value = "{id}/submit",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@PathVariable String id) {
        try {
            leaveRequestService.submit(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(value = "{id}/reject",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reject(@PathVariable String id) {
        try {
            leaveRequestService.reject(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(value = "{id}/approve",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approve(@PathVariable String id) {
        try {
            leaveRequestService.approve(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(value = "{id}/cancel",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String id) {
        try {
            leaveRequestService.cancel(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            leaveRequestService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
