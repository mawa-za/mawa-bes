package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.leave.request.LeaveRequestCancelDto;
import za.co.mawa.bes.dto.leave.request.LeaveRequestEditDto;
import za.co.mawa.bes.dto.leave.request.LeaveRequestInboundDto;
import za.co.mawa.bes.service.LeaveRequestService;
import za.co.mawa.bes.service.TransactionService;


@RestController
@CrossOrigin
@RequestMapping(value = "leave-request")
public class LeaveRequestController {
    Gson gson = new Gson();
    @Autowired
    LeaveRequestService leaveRequestService;
    @Autowired
    TransactionService transactionService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestBody LeaveRequestInboundDto leaveRequestInboundDto) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 leave request endpoint so profiles, balances and approval rules are applied"));
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
            var requests = leaveRequestService.search();
            if (status != null && !status.isBlank()) {
                requests = requests.stream()
                        .filter(request -> request.getStatus() != null
                                && status.equalsIgnoreCase(request.getStatus().getCode()))
                        .toList();
            }
            return ResponseEntity.ok(gson.toJson(requests));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable String id,@RequestBody LeaveRequestEditDto leaveRequestEditDto) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 leave request endpoint so profile and balance rules are revalidated"));
    }

    @RequestMapping(value = "{id}/submit",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 leave submission endpoint so the configured approval workflow is enforced"));
    }

    @RequestMapping(value = "{id}/reject",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reject(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Leave decisions must be actioned through the approval inbox"));
    }

    @RequestMapping(value = "{id}/approve",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approve(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Leave decisions must be actioned through the approval inbox"));
    }

    @RequestMapping(value = "{id}/cancel",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String id, @RequestBody LeaveRequestCancelDto leaveRequestCancelDto) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 leave cancellation endpoint so approved balance entries are reversed safely"));
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of("message", "Use the v2 leave request endpoint; only pending requests can be deleted"));
    }
}
