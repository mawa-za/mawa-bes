package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;

import java.util.List;

@RestController
@CrossOrigin
public class CalculatorController {
    Gson gson = new Gson();
    @RequestMapping(value = "/calculate", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteQuotation(@RequestBody List<LineItemDto> items) {
        try {
//            pricingService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
