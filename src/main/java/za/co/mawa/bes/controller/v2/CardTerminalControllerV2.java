package za.co.mawa.bes.controller.v2;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CardTerminalRequest;
import za.co.mawa.bes.entity.v2.CardTerminalEntity;
import za.co.mawa.bes.service.v2.CardTerminalService;
import java.util.List;
@CrossOrigin @RestController @RequiredArgsConstructor @RequestMapping("v2/card-terminals")
public class CardTerminalControllerV2 {
    private final CardTerminalService service;
    @GetMapping public List<CardTerminalEntity> list(@RequestParam(defaultValue="false") boolean activeOnly){return service.list(activeOnly);}
    @PostMapping public CardTerminalEntity create(@RequestBody CardTerminalRequest request){return service.save(null,request);}
    @PutMapping("/{id}") public CardTerminalEntity update(@PathVariable String id,@RequestBody CardTerminalRequest request){return service.save(id,request);}
}
