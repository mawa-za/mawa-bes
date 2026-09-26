package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.LegalPracticeService;
import java.util.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/legal")
public class LegalPracticeControllerV2 {
    private final LegalPracticeService service;
    @GetMapping("/dashboard") public Map<String,Object> dashboard(){ return service.dashboard(); }
    @GetMapping("/{resource}") public List<Map<String,Object>> list(@PathVariable String resource,@RequestParam(required=false) String caseId){ return service.list(resource,caseId); }
    @GetMapping("/{resource}/{id}") public Map<String,Object> get(@PathVariable String resource,@PathVariable String id){ return service.get(resource,id); }
    @PostMapping("/{resource}") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@PathVariable String resource,@RequestBody Map<String,Object> body){ return service.create(resource,body); }
    @PutMapping("/{resource}/{id}") public Map<String,Object> update(@PathVariable String resource,@PathVariable String id,@RequestBody Map<String,Object> body){ return service.update(resource,id,body); }
    @DeleteMapping("/{resource}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String resource,@PathVariable String id){ service.delete(resource,id); }
}
