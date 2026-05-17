package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.DependentCreateDto;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.RepresentativeCreateDto;
import za.co.mawa.bes.dto.TombstoneRecipientDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.membership.MembershipEditDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.TransactionPartnerAddException;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class GroupSocietyController {
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

    @RequestMapping(value = "/v2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGroupSocietyUsingView() {
        try {
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.GROUP_SOCIETY);
            List<TransactionViewEntity> transactionViewEntities = transactionService.searchV2(transactionViewDto);

            return ResponseEntity.ok(gson.toJson(transactionViewEntities));
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

    @RequestMapping(value = "{id}/representative", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "{id}/representative", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "{id}/representative/{representativeId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRepresentative(@PathVariable String id, @PathVariable String representativeId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.REPRESENTATIVE);
            transactionPartnerDto.setPartner(representativeId);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
