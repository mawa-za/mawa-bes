package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.membership.MembershipEditDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.MembershipService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.util.*;

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
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
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
                creationDate.setType(DateType.JOINED);
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

    @RequestMapping(method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMemberships(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String partnerFunction,
                                            @RequestParam(required = false) String partnerNo,
                                            @RequestParam(required = false) String idNumber) {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            String partner = partnerNo == null ? "":partnerNo;
            String function = partnerFunction == null ? "":partnerFunction;
            String idNo = idNumber == null ? "":idNumber;
            List<TransactionQueryResultDto> results = new ArrayList<>();
            if(idNumber != "" && function != ""){
                for(PartnerIdentityEntity identityEntity: partnerIdentityRepository.findPartnerIdentityByValue(idNumber)){
                    transactionQueryDto.setPartnerNo(identityEntity.getPartner());
                    transactionQueryDto.setPartnerFunction(function);
                    List<TransactionQueryResultDto> result = new ArrayList<>();
                    transactionQueryDto.setType(TransactionType.MEMBERSHIP);
                    result = transactionService.search(transactionQueryDto);
                    results.addAll(result);
                }
            }
            else{
                if(status != null && status != "")
                {
                    transactionQueryDto.setStatus(status);
                }
                if(partner != "" && function != "")
                {
                    transactionQueryDto.setPartnerNo(partner);
                    transactionQueryDto.setPartnerFunction(function);
                }
                transactionQueryDto.setType(TransactionType.MEMBERSHIP);
                results = transactionService.search(transactionQueryDto);
            }
            return ResponseEntity.ok(gson.toJson(results));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMembership(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if(transactionDto.getType().equalsIgnoreCase(TransactionType.MEMBERSHIP))
            {
                String productId = null;
                for(TransactionItemDto item:transactionService.getItems(transactionDto.getId())){
                   int number =  item.getValidTo().compareTo(new Date());
                 if(number > 0){
                    productId = item.getProduct();
                 }
                }
               ProductDto productDto =  productService.getOptionalById(productId);
                if(productDto != null)
                {
                    transactionDto.setProductDetails(productDto);
                }
                return ResponseEntity.ok(gson.toJson(transactionDto));
            }
            else {
              return  ResponseEntity.ok(gson.toJson(null));
            }
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editMembership(@PathVariable String id, @RequestBody MembershipEditDto membershipDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            TransactionPartnerEdit partnerEdit = new TransactionPartnerEdit();
            boolean edited = false;
            if(membershipDto.getStatus() != null && membershipDto.getStatus() != ""){
                transactionEditDto.setStatus(membershipDto.getStatus());
            }
            if(membershipDto.getStatusReason() != null && membershipDto.getStatusReason() != ""){
                transactionEditDto.setStatusReason(membershipDto.getStatusReason());
            }
            if(transactionEditDto.getStatusReason() != null || transactionEditDto.getStatus() != null)
            {
                transactionEditDto.setId(id);
              transactionService.edit(transactionEditDto);
            }
            if(membershipDto.getSalesRepresentativeId() != null && membershipDto.getSalesRepresentativeId() != ""){
              partnerEdit.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
              partnerEdit.setTransaction(id);
              partnerEdit.setParnter(membershipDto.getSalesRepresentativeId());
              edited = transactionService.partnerEdit(partnerEdit);
            }
            if(membershipDto.getPremium() != null && membershipDto.getProductId() != null && membershipDto.getProductId() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setUnitPrice(membershipDto.getPremium());
                edited = transactionService.editItem(editDto);
            }
            if(membershipDto.getProductId() != null && membershipDto.getProductId() != "" && membershipDto.getPreviousProduct() != null && membershipDto.getPreviousProduct() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setPreviousProduct(membershipDto.getPreviousProduct());
                edited = transactionService.editItem(editDto);
            }
            return ResponseEntity.ok().body(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDependent(@PathVariable String id, @RequestBody DependentDto dependentDto) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentDto.getId());
            transactionService.addPartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent/{dependentId}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDependent(@PathVariable String id, @PathVariable String dependentId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentId);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/claims", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

}
