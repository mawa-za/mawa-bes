package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.co.mawa.bes.dto.BankFileXmlCreateDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.service.BankFileXmlService;

@RestController
@CrossOrigin
@RequestMapping(value = "bankFileXml")
public class BankFileXmlController {

    Gson gson = new Gson();

    @Autowired
    BankFileXmlService bankFileXmlService;
    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateBankFile(@RequestBody BankFileXmlCreateDto bankFileCreateXmlDto) {
        try {
            bankFileXmlService.createBankFile(bankFileCreateXmlDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
