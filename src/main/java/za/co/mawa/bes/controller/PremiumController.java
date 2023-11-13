package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.cashup.CashupCreateDto;
import za.co.mawa.bes.dto.premium.PremiumCreateDto;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.service.CashupService;
import za.co.mawa.bes.service.PremiumService;
import za.co.mawa.bes.service.ReceiptService;
import za.co.mawa.bes.utils.TenderType;

import java.math.BigDecimal;
import java.util.ArrayList;

@RestController
@CrossOrigin
@RequestMapping(value = "premium")
public class PremiumController {
    Gson gson = new Gson();

    @Autowired
    PremiumService premiumService;
    @Autowired
    CashupService cashupService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postPremium(@RequestBody PremiumCreateDto premiumCreateDto) {
        try {
            PremiumDto premiumDto = premiumService.create(premiumCreateDto);
            if (premiumCreateDto.getTenderType().equals(TenderType.EFT) || premiumCreateDto.getTenderType().equals(TenderType.CARD)){
                CashupCreateDto cashupCreateDto = new CashupCreateDto();
                cashupCreateDto.setEmployeeResponsibleId(premiumDto.getEmployeeResponsible());
                cashupCreateDto.setSalesArea(premiumCreateDto.getLocation());
                cashupCreateDto.setAmount(new BigDecimal(premiumCreateDto.getAmount()));
                cashupCreateDto.setReceipts(new ArrayList<>());
                cashupService.createNoCash(cashupCreateDto);
            }
            return ResponseEntity.ok(gson.toJson(premiumDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPremium(@PathVariable String id) {
        try {
            PremiumDto premiumDto = premiumService.get(id);
            return ResponseEntity.ok(gson.toJson(premiumDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value = "/premiums", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPremiums(@RequestParam(required = false) String receiptType,
                                         @RequestParam(required = false) String invoiceNumber,
                                         @RequestParam(required = false) String membershipId,
                                         @RequestParam(required = false) String membershipPeriod,
                                         @RequestParam(required = false) String tenderType,
                                         @RequestParam(required = false) boolean notCashed,
                                         @RequestParam(name = "user", required = false) String createdBy) {
        try {
            PremiumSearchDto search = new PremiumSearchDto();
            if (membershipId != null && membershipId != "") {
                search.setMembershipId(membershipId);
            }
            if (membershipPeriod != null && membershipPeriod != "") {
                search.setMembershipPeriod(membershipPeriod);
            }
            if (tenderType != null && tenderType != "") {
                search.setTenderType(tenderType);
            }
            if (createdBy != null && createdBy != "") {
                search.setEmployeeResponsible(createdBy);
            }
            ArrayList<PremiumDto> premiumDtoArrayList = new ArrayList<>();
            if (notCashed) {
                premiumDtoArrayList = premiumService.getReceiptsX(search);
            } else {
                premiumDtoArrayList = premiumService.getReceipts(search);
            }
            return ResponseEntity.ok(gson.toJson(premiumDtoArrayList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
