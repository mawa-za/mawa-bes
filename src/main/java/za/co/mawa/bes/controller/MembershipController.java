package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.service.MembershipService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(value = "membership")
public class MembershipController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postMembership(@RequestBody MembershipCreateDto membershipCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.MEMBERSHIP);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            if (membershipCreateDto.getProductId() != null) {
                ProductDto productDto = productService.get(membershipCreateDto.getProductId());
                TransactionItemDto transactionItemDto = new TransactionItemDto();
                transactionItemDto.setTransaction(transactionDto.getId());
                transactionItemDto.setProduct(membershipCreateDto.getProductId());
                transactionItemDto.setProduct(productDto.getId());
                transactionItemDto.setUnitPrice(productDto.getSellingPrice());
                transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
                transactionItemDto.setQuantity(new BigDecimal("1"));
                transactionService.addItem(transactionItemDto);
            }

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);

            if (membershipCreateDto.getDateJoined() != null) {
                TransactionDateDto Date = new TransactionDateDto();
                creationDate.setTransaction(transactionDto.getId());
                creationDate.setType(DateType.CREATED);
                creationDate.setValue(membershipCreateDto.getDateJoined());
                transactionService.addDate(creationDate);
            }

            if (membershipCreateDto.getMemberId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.MAINMEMBER);
                transactionPartnerDto.setPartner(membershipCreateDto.getMemberId());
                transactionService.addPartner(transactionPartnerDto);
            }

            if (membershipCreateDto.getSalesRepresentativeId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(membershipCreateDto.getSalesRepresentativeId());
                transactionService.addPartner(transactionPartnerDto);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

//    @RequestMapping(method = RequestMethod.GET)
//    public ResponseEntity<?> getMemberships() {
//        try {
//            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
//            transactionQueryDto.setType(TransactionType.MEMBERSHIP);
//            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getMembership(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editMembership(@PathVariable String id, @RequestBody MembershipDto membershipDto) {
        try {
            TransactionDto transactionDto = new TransactionDto();
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.POST)
    public ResponseEntity<?> addDependent(@PathVariable String id, @RequestBody DependentDto dependentDto) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentDto.getId());
            transactionService.addPartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.GET)
    public ResponseEntity<?> getDependent(@PathVariable String id) {
        try {
            List<DependentDto> dependentDtoList = new ArrayList<>();
            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id).stream()
                    .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.DEPENDENT))
                    .toList();
            for (TransactionPartnerDto transactionPartnerDto : transactionPartnerDtoList) {
                PartnerDto partnerDto = partnerService.get(transactionPartnerDto.getPartner());
                if (partnerDto != null) {
                    DependentDto dependentDto = new DependentDto();
                    dependentDto.setIdType(partnerDto.getIdType());
                    dependentDto.setIdNumber(partnerDto.getIdNumber());
                    dependentDto.setLastName(partnerDto.getName1());
                    dependentDto.setFirstName(partnerDto.getName2());
                    dependentDto.setMiddleName(partnerDto.getName3());
                    dependentDtoList.add(dependentDto);
                }
            }
            return ResponseEntity.ok(gson.toJson(dependentDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/dependent/{dependentId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> addDependent(@PathVariable String id, @PathVariable String dependentId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentId);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/claims", method = RequestMethod.GET)
    public ResponseEntity<?> getClaims(@PathVariable String id) {
        try {
            List<ClaimDto> claimDtoList = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (TransactionQueryResultDto transactionDto : transactionService.search(transactionQueryDto)) {
//                claimDtoList.add(getClaimData(transactionDto.getId()));
            }
            return ResponseEntity.ok(gson.toJson(claimDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
