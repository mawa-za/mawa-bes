package za.co.mawa.bes.controller.v2;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.ErrorResponse;
import za.co.mawa.bes.dto.PrintJobRequest;
import za.co.mawa.bes.dto.premium.PremiumCreateDto;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.premium.PremiumInboundDto;
import za.co.mawa.bes.dto.premium.PremiumSearchDto;
import za.co.mawa.bes.service.CashupService;
import za.co.mawa.bes.service.DepositService;
import za.co.mawa.bes.service.PremiumService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/premium")
public class PremiumControllerV2 {
    Gson gson = new Gson();
    @Autowired
    PremiumService premiumService;
    @Autowired
    CashupService cashupService;
    @Autowired
    DepositService depositService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postPremium(@RequestBody PremiumCreateDto premiumCreateDto)  throws RuntimeException{
        try {

            List<PremiumDto> premiums = premiumService.findByMembership(premiumCreateDto.getMembershipId());

            List<PremiumDto> periodPremiums = premiums.stream()
                    .filter(a -> Objects.equals(a.getMembershipPeriod(), premiumCreateDto.getMembershipPeriod()))
                    .toList();
            if (!periodPremiums.isEmpty()) {
                throw new Exception("Error: Receipt exists for period");
            }

            if (!premiums.stream().filter(a -> Objects.equals(a.getMembershipPeriod(), premiumCreateDto.getMembershipPeriod()))
                    .toList().isEmpty())
            {
                throw new Exception("Error: Receipt number already exists.");
            }
            PremiumDto premiumDto = premiumService.create(premiumCreateDto);
            return ResponseEntity.ok(gson.toJson(premiumDto));
        } catch (Exception exception) {
//            throw new RuntimeException(exception.getMessage());
            ErrorResponse error = new ErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
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

    @RequestMapping(value = "{id}/print", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> print(HttpServletRequest request, @PathVariable String id) {
        try {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            PremiumDto premiumDto = premiumService.get(id);
            PrintJobRequest printJobRequest = new PrintJobRequest();
            printJobRequest.setPrinterId(ipAddress);
            printJobRequest.setObjectId(id);
            premiumService.print(printJobRequest);
            return ResponseEntity.ok(gson.toJson(premiumDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updatePremium(@RequestBody PremiumInboundDto premiumInboundDto, @PathVariable String id) {
        try {
            premiumInboundDto.setId(id);
            premiumService.update(premiumInboundDto);
            return ResponseEntity.ok("Updated successfully");
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update failed");
        }

    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePremium(@PathVariable String id) {
        try {
            PremiumInboundDto premiumInboundDto = new PremiumInboundDto();
            premiumInboundDto.setId(id);
            premiumService.delete(premiumInboundDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPremiums(@RequestParam(required = false) String membershipId) {
        try {
            return ResponseEntity.ok(gson.toJson(premiumService.findByMembership(membershipId)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
