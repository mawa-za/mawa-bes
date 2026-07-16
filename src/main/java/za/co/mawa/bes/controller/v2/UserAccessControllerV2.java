package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.UserAccessService;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/access")
public class UserAccessControllerV2 {
    @Autowired private UserAccessService accessService;
    @GetMapping("/profile") public ResponseEntity<?> profile(){ return ResponseEntity.ok(accessService.profile()); }
    @PostMapping("/session/close") public ResponseEntity<?> close(){ accessService.audit("SESSION_CLOSED","SESSION",null,null,null); return ResponseEntity.ok(Map.of("closed",true)); }
}
