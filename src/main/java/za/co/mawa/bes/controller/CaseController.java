package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LitigantCreateDto;
import za.co.mawa.bes.dto.RepresentativeCreateDto;
import za.co.mawa.bes.dto.cas.CaseCreateDto;
import za.co.mawa.bes.dto.cas.CaseDto;
import za.co.mawa.bes.dto.cas.CaseQueryDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.participant.ParticipantCreateDto;
import za.co.mawa.bes.dto.participant.ParticipantDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "case")
public class CaseController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    CaseService caseService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;

    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postCase(@RequestBody CaseCreateDto caseCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(caseService.create(caseCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCase(@PathVariable String id) {
        try {
            CaseDto caseDto = caseService.get(id);
            return ResponseEntity.ok(gson.toJson(caseDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchCase() {
        try {
            CaseQueryDto caseQueryDto = new CaseQueryDto();
            return ResponseEntity.ok(gson.toJson(caseService.search(caseQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}/participant", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addLitigant(@PathVariable String id, @RequestBody ParticipantCreateDto participantCreateDto) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(participantCreateDto.getFunction());
            transactionPartnerDto.setPartner(participantCreateDto.getPartner());
            transactionService.addPartner(transactionPartnerDto);

            //create a link for participant and legal rep
            try {
                TransactionLinkDto link = new TransactionLinkDto();
                link.setTransaction1(participantCreateDto.getPartner());
                link.setTransaction2(participantCreateDto.getLegalRepresentative());
                link.setType(TransactionType.LEGAL_REPRESENTATIVE_LINK);
                link.setCreateBy("");
                transactionService.addLink(link);

            }catch (Exception e){}

            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/participant", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLitigant(@PathVariable String id) {
        try {
            List<ParticipantDto> participantDtoList = new ArrayList<>();
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    ParticipantDto participantDto = new ParticipantDto();
                    participantDto.setPartner(partnerService.get(transactionPartnerDto.getPartner()));
                    participantDto.setFunction(fieldOptionService.getFieldOption(Field.PARTNER_FUNCTION, transactionPartnerDto.getFunction()));
                    participantDtoList.add(participantDto);
                } catch (PartnerNotFoundException e) {

                }
            }
            return ResponseEntity.ok(gson.toJson(participantDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/participant/{participant}/{function}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addLitigant(@PathVariable String id, @PathVariable String participant, @PathVariable String function) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(function);
            transactionPartnerDto.setPartner(participant);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
