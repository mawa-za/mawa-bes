package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.RepresentativeCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.GroupSocietyService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.PartnerFunction;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "case")
public class CaseController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    GroupSocietyService groupSocietyService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postGroupSociety(@RequestBody GroupSocietyCreateDto groupSocietyCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(groupSocietyService.create(groupSocietyCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGroupSociety(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(groupSocietyService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchGroupSociety() {
        try {
            GroupSocietyQueryDto groupSocietyQueryDto = new GroupSocietyQueryDto();
            return ResponseEntity.ok(gson.toJson(groupSocietyService.search(groupSocietyQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/litigant", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRepresentative(@PathVariable String id, @RequestBody RepresentativeCreateDto representativeCreateDto) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.REPRESENTATIVE);
            transactionPartnerDto.setPartner(representativeCreateDto.getPartnerId());
            if (representativeCreateDto.getDateAdded() != null) {
                transactionPartnerDto.setDateAdded(representativeCreateDto.getDateAdded());
            }
            if (representativeCreateDto.getDateEffective() != null) {
                transactionPartnerDto.setDateEffective(representativeCreateDto.getDateEffective());
            }
            transactionService.addPartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/litigant", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRepresentative(@PathVariable String id) {
        try {
            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id);
            ArrayList<PartnerDto> representativeDtoList = new ArrayList<>();
            for (TransactionPartnerDto partners : transactionPartnerDtoList) {
                if (partners.getFunction().equalsIgnoreCase(PartnerFunction.REPRESENTATIVE)) {
                    try {
                        representativeDtoList.add(partnerService.get(partners.getPartner()));
                    } catch (Exception ex) {

                    }
                }
            }
            return ResponseEntity.ok(gson.toJson(representativeDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/litigant/{litigant}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRepresentative(@PathVariable String id, @PathVariable String litigant) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.REPRESENTATIVE);
            transactionPartnerDto.setPartner(litigant);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
