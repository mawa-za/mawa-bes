package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.service.BankAccountService;

@RestController
@CrossOrigin
@RequestMapping(value = "bank-account")
public class BankAccountController {
    @Autowired
    BankAccountService bankAccountService;
    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAttachment(@RequestBody BankAccountCreateDto bankAccountCreateDto) {
        try {
            bankAccountService.save(bankAccountCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
