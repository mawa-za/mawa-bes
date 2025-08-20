package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.cas.CaseQueryDto;
import za.co.mawa.bes.repository.TransactionViewRepository;
import za.co.mawa.bes.service.CaseService;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/case")
public class CaseControllerV2 {
    Gson gson = new Gson();
    @Autowired
    CaseService caseService;
    @Autowired
    TransactionViewRepository transactionViewRepository;
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchCase() {
        try {
            return ResponseEntity.ok(gson.toJson(transactionViewRepository.findAllByType(TransactionType.CASE)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
