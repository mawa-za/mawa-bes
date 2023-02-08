//package za.co.mawa.bes.controller;
//
//import com.nimbusds.jose.shaded.gson.Gson;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import za.co.mawa.bes.dto.*;
//import za.co.mawa.bes.service.MembershipService;
//import za.co.mawa.bes.service.TransactionService;
//import za.co.mawa.bes.utils.DateType;
//import za.co.mawa.bes.utils.PartnerFunction;
//import za.co.mawa.bes.utils.TransactionType;
//
//@RestController
//@CrossOrigin
//public class PolicyController {
//    @Autowired
//    TransactionService transactionService;
//    @Autowired
//    MembershipService membershipService;
//    Gson gson = new Gson();
//
//    @RequestMapping(value = "/membership", method = RequestMethod.POST)
//    public ResponseEntity<?> postMembership(@RequestBody MembershipCreateDto membershipCreateDto) {
//        try {
//
//            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
//            transactionCreateDto.setType(TransactionType.MEMBERSHIP);
//            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
//
//            TransactionDateDto creationDate = new TransactionDateDto();
//            creationDate.setTransaction(transactionDto.getId());
//            creationDate.setType(DateType.CREATED);
//            transactionService.addDate(creationDate);
//
////            TransactionDateDto Date = new TransactionDateDto();
////            creationDate.setTransaction(transactionDto.getId());
////            creationDate.setType(DateType.CREATED);
////            transactionService.addDate(creationDate);
//
//            if (membershipCreateDto.getMemberId() != null) {
//                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
//                transactionPartnerDto.setTransaction(transactionDto.getId());
//                transactionPartnerDto.setFunction(PartnerFunction.MAINMEMBER);
//                transactionPartnerDto.setPartner(membershipCreateDto.getMemberId());
//                transactionService.addPartner(transactionPartnerDto);
//            }
//
//            if (membershipCreateDto.getSalesRepresentativeId() != null) {
//                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
//                transactionPartnerDto.setTransaction(transactionDto.getId());
//                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
//                transactionPartnerDto.setPartner(membershipCreateDto.getSalesRepresentativeId());
//                transactionService.addPartner(transactionPartnerDto);
//            }
//
//            return ResponseEntity.ok(gson.toJson(transactionService.create(transactionCreateDto)));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @RequestMapping(value = "/membership", method = RequestMethod.GET)
//    public ResponseEntity<?> getMemberships(@RequestBody MembershipQueryDto membershipQueryDto) {
//        try {
//            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
//            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @RequestMapping(value = "/membership/{id}", method = RequestMethod.GET)
//    public ResponseEntity<?> getMembership(@PathVariable String id) {
//        try {
//            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @RequestMapping(value = "/membership/{id}", method = RequestMethod.PUT)
//    public ResponseEntity<?> editMembership(@PathVariable String id, @RequestBody MembershipDto membershipDto) {
//        try {
//            TransactionDto transactionDto = new TransactionDto();
//            transactionService.edit(transactionDto);
//            return ResponseEntity.ok().build();
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @RequestMapping(value = "/membership/{id}", method = RequestMethod.DELETE)
//    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
//        try {
//            transactionService.delete(id);
//            return ResponseEntity.ok().build();
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//}
