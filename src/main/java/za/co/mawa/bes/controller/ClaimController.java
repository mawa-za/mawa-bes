package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.ClaimQueryDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.service.ClaimService;

@RestController
@CrossOrigin
public class ClaimController {
    Gson gson = new Gson();
    @Autowired
    ClaimService claimService;
    @RequestMapping(value = "/claim", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters(@RequestBody ClaimQueryDto claimQueryDto) {
        return ResponseEntity.ok(gson.toJson(claimService.search(claimQueryDto)));
    }
}
