package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.service.PricingEngineService;
import za.co.mawa.bes.service.PricingService;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "pricing-engine")
public class PricingEngineController {
    @Autowired
    PricingEngineService pricingEngineService;
    Gson gson = new Gson();
//    @RequestMapping(method = RequestMethod.POST)
//    public ResponseEntity<?> calculatePricing(@RequestBody List<LineItemDto> items) {
//        try {
//            PricingDto pricingDto = pricingEngineService.calculate(items);
//            return ResponseEntity.ok(gson.toJson(pricingDto));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
}
