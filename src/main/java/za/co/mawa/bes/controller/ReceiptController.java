package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.cashup.CashupCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.repository.ReceiptRepository;
import za.co.mawa.bes.service.CashupService;
import za.co.mawa.bes.service.ReceiptService;
import za.co.mawa.bes.utils.TenderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "receipt")
public class ReceiptController {
    Gson gson = new Gson();

    @Autowired
    ReceiptService receiptService;
    @Autowired
    CashupService cashupService;
    @Autowired
    private ReceiptRepository receiptRepository;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createReceipt(@RequestBody ReceiptCreateDto receiptCreateDto) {
        try {
            ReceiptDto receiptDto = receiptService.createReceipt(receiptCreateDto);
            if (receiptCreateDto.getTenderType().equals(TenderType.EFT) || receiptCreateDto.getTenderType().equals(TenderType.CARD)){
                CashupCreateDto cashupCreateDto = new CashupCreateDto();
                cashupCreateDto.setEmployeeResponsibleId(receiptDto.getCreatedBy().getId());
                cashupCreateDto.setSalesArea(receiptCreateDto.getLocation());
                cashupCreateDto.setAmount(new BigDecimal(receiptCreateDto.getAmount()));
                List<String> receipts = new ArrayList<>();
                receipts.add(receiptDto.getId());
                cashupCreateDto.setReceipts(receipts);
                cashupService.createNoCash(cashupCreateDto);
            }
            return ResponseEntity.ok(gson.toJson(receiptDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReceipt(@PathVariable String id) {
        try {
            ReceiptDto receiptDto = receiptService.getReceipt(id);
            return ResponseEntity.ok(gson.toJson(receiptDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping( method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReceipts(@RequestParam(required = false) String receiptType,
                                         @RequestParam(required = false) String location,
                                         @RequestParam(required = false) String transaction,
                                         @RequestParam(required = false) String tenderType,
                                         @RequestParam(required = false) boolean notCashed,
                                         @RequestParam(name = "user", required = false) String createdBy) {
        try {
            ReceiptSearchDto search = new ReceiptSearchDto();
            if (receiptType != null && receiptType != "") {
                search.setReceiptType(receiptType);
            }
            if (location != null && location != "") {
                search.setLocation(location);
            }
            if (transaction != null && transaction != "") {
                search.setTransaction(transaction);
            }
            if (tenderType != null && tenderType != "") {
                search.setTenderType(tenderType);
            }
            if (createdBy != null && createdBy != "") {
                search.setCreatedBy(createdBy);
            }
            ArrayList<ReceiptDto> receipts = new ArrayList<>();
            if (notCashed) {
                receipts = receiptService.getReceiptsX(search);
            } else {
                receipts = receiptService.getReceipts(search);
            }
            return ResponseEntity.ok(gson.toJson(receipts));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
