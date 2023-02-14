package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PricingDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class CalculatorController {
    Gson gson = new Gson();
    @RequestMapping(value = "/calculate-pricing", method = RequestMethod.POST)
    public ResponseEntity<?> calculatePricing(@RequestBody List<LineItemDto> items) {
        try {
            List<LineItemDto> lineItemDtoList = new ArrayList<>();
            PricingDto pricingDto = new PricingDto();
            pricingDto.setVATPercentage(new BigDecimal("15"));
            for (LineItemDto lineItemDto:items){
                lineItemDto.setLineTotal(lineItemDto.getQuantity().multiply(lineItemDto.getUnitPrice()));
                pricingDto.setTotalExcVat(pricingDto.getTotalExcVat().add(lineItemDto.getLineTotal()));
                pricingDto.setVATAmount(pricingDto.getTotalExcVat().multiply(pricingDto.getVATPercentage().divide(new BigDecimal("100"))));
                pricingDto.setTotalIncVat(pricingDto.getTotalExcVat().add((pricingDto.getVATAmount())));
                lineItemDtoList.add(lineItemDto);
            }
            pricingDto.setItems(lineItemDtoList);
            return ResponseEntity.ok(gson.toJson(pricingDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
