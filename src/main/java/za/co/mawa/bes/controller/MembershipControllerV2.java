package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@CrossOrigin
@RequestMapping(value = "V2/membership")
public class MembershipControllerV2 {
    Gson gson = new Gson();
    @Autowired
    TransactionService transactionService;

    @RequestMapping(value = "mawa-pay", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMemberships(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String mainPartner,
                                              @RequestParam(required = false) String employeeResponsibleName,
                                              @RequestParam(required = false) String creationDate,
                                              @RequestParam(required = false) String idNumber) {
        try {
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.MEMBERSHIP);

            if(status != null && status != "") {
                transactionViewDto.setStatus(status);
            }

            if(employeeResponsibleName != null && employeeResponsibleName != "") {
                transactionViewDto.setEmployeeResponsibleName(employeeResponsibleName);
            }

            if(mainPartner != null && mainPartner != "") {
                transactionViewDto.setMainPartner(mainPartner);
            }

            if(idNumber != null && idNumber != "") {
                transactionViewDto.setIdNumber(idNumber);
            }

            if (creationDate != null) {

                // Define the formatter for the input string
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

                // Parse the string into Date object
                Date date = formatter.parse(creationDate);

                transactionViewDto.setCreationDate(date);
            }

            return ResponseEntity.ok(gson.toJson(transactionService.searchV2(transactionViewDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
