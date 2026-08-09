package za.co.mawa.bes.controller.v2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.PaymentAccountConfigurationService;
import java.util.*;
@RestController
@RequestMapping({"/v2/payment-account-configurations", "/v2/payment-account-configuration"})
@RequiredArgsConstructor
public class PaymentAccountConfigurationController {
 private final PaymentAccountConfigurationService service;
 @GetMapping public List<Map<String,Object>> list(){return service.list();}
 @PostMapping public Map<String,Object> save(@RequestBody Map<String,Object> body){return service.save(body);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> deactivate(@PathVariable String id){service.deactivate(id);return ResponseEntity.noContent().build();}
}
