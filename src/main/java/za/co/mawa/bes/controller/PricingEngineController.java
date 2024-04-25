package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.PricingInboundDto;
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
    @RequestMapping(method = RequestMethod.POST,  produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> calculate(@RequestBody PricingInboundDto pricingInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(pricingEngineService.simulate(pricingInboundDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
