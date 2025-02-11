package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.*;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@RestController
@CrossOrigin
@RequestMapping(value = "claim")
public class ClaimController {
    @Autowired
    ClaimService claimService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    SettingService settingService;

    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postClaim(@RequestBody ClaimCreateDto claimCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(claimService.create(claimCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaims(@RequestParam(required = false) String no,
                                       @RequestParam(required = false) String claimant,
                                       @RequestParam(required = false) String deceased,
                                       @RequestParam(required = false) String member,
                                       @RequestParam(required = false) String membership,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String deathDate,
                                       @RequestParam(required = false) String burialDate,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String parent) {
        try {

            ClaimQueryDto claimQueryDto = new ClaimQueryDto();
            if (status != null && status != "") {
                claimQueryDto.setStatus(status);
            }
            if (no != null && no != "") {
                claimQueryDto.setNumber(no);
            }
            if (claimant != null & claimant != "") {
                claimQueryDto.setClaimant(claimant);
            }
            if (deceased != null && deceased != "") {
                claimQueryDto.setDeceased(deceased);
            }
            if (member != null && member != "") {
                claimQueryDto.setMember(member);
            }
            if (type != null && type != "") {
                claimQueryDto.setType(type);
            }
            if (deathDate != null && deathDate != "") {
                Date death = new SimpleDateFormat("yyyy-MM-dd").parse(deathDate);
                claimQueryDto.setDeathDate(death);
            }
            if (burialDate != null && burialDate != "") {
                Date burial = new SimpleDateFormat("yyyy-MM-dd").parse(burialDate);
                claimQueryDto.setBurialDate(burial);
            }
            if (membership != null && membership != "") {
                claimQueryDto.setMembership(membership);
            }
            if (parent != null && parent != "") {
                claimQueryDto.setParent(parent);
            }
            return ResponseEntity.ok(gson.toJson(claimService.search(claimQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "v2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaimsV2(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String mainPartner,
                                         @RequestParam(required = false) String employeeResponsibleName,
                                         @RequestParam(required = false) String creationDate,
                                         @RequestParam(required = false) String idNumber) {
        try {
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.CLAIM);

            if (status != null && status != "") {
                transactionViewDto.setStatus(status);
            }

            if (employeeResponsibleName != null && employeeResponsibleName != "") {
                transactionViewDto.setEmployeeResponsibleName(employeeResponsibleName);
            }

            if (mainPartner != null && mainPartner != "") {
                transactionViewDto.setMainPartner(mainPartner);
            }

            if (idNumber != null && idNumber != "") {
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

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaim(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(claimService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/claimByMember/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaimByMember(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            List<ClaimOutboundDto> claimOutboundDtoList = new ArrayList<>();

            for (TransactionLinkDto transactionLinkDto : transactionService.getLinks(id)) {
                claimOutboundDtoList.add(claimService.get(transactionLinkDto.getTransaction2()));
            }
            return ResponseEntity.ok(gson.toJson(claimOutboundDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/approve", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveClaim(@PathVariable String id,
                                          @RequestParam(required = false) String statusReason,
                                          @RequestParam(required = false) String description) {
        try {
            claimService.approve(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectClaim(@PathVariable String id,
                                         @RequestParam(required = true) String statusReason,
                                         @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(ClaimStatus.REJECTED);
            transactionEditDto.setStatusReason(statusReason);
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/submit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitClaim(@PathVariable String id) {
        try {
            claimService.submit(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dispute", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disputeClaim(@PathVariable String id,
                                          @RequestBody ClaimDisputeDto dispute) {
        try {
            dispute.setClaimId(id);
            claimService.dispute(dispute);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelClaim(@PathVariable String id,
                                         @RequestBody ClaimCancelDto claimCancelDto) {
        try {
            claimCancelDto.setClaimId(id);
            claimService.cancel(claimCancelDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/complete", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> complete(@PathVariable String id,
                                      @RequestParam(required = false) String statusReason,
                                      @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.PROCESSED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionService.get(transactionEditDto.getId())));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editClaim(@PathVariable String id, @RequestBody ClaimEditDto claimDto) {
        try {

            return ResponseEntity.ok(gson.toJson(claimService.edit(id, claimDto)));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteClaim(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/bankDetails", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postClaimBankDetails(@RequestBody TransactionAccountDto transactionAccountDto) {
        try {
            transactionService.addBankAccount(transactionAccountDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/payment-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generatePaymentRequests(@PathVariable String id) {
        try {
            String claimId = id;
            ClaimOutboundDto claim = claimService.get(claimId);
            if (claim.getType().getCode().equals("CASH")) {

                PaymentRequestCreateDto paymentRequest = new PaymentRequestCreateDto();
                paymentRequest.setPaymentMethod(claim.getPaymentMethod().getCode());
                paymentRequest.setPaymentReason(claim.getType().getCode() + "-CLAIM");
                paymentRequest.setReference("CASHCLAIM" + claim.getNumber());
                paymentRequest.setDueDate(new Date());
                paymentRequest.setRecipientId(claim.getMember().getId());
                try {
                    paymentRequest.setAmount(claim.getPaidOutAmount().getAmount());
                } catch (Exception ex) {
                    paymentRequest.setAmount(new BigDecimal("0"));
                }
                try {
                    paymentRequest.setBranch(claim.getBranch().getCode());
                } catch (Exception e) {
                    paymentRequest.setBranch("MODJADJISKLOOF");
                }
                paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                String paymentRequestId = paymentRequestService.create(paymentRequest);
                if (claim.getPaymentMethod().getCode().equals("EFT")) {
                    BankAccountDto bankAccountDto = bankAccountService.getList(claimId).iterator().next();
                    BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                    bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccount.setObjectId(paymentRequestId);
                    bankAccountService.add(bankAccount);
//                    paymentRequest.setBankAccount(bankAccount);
                }

                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }
            }

            if (claim.getType().getCode().equals("FUNERAL")) {
                PaymentRequestCreateDto paymentRequest = new PaymentRequestCreateDto();
                paymentRequest.setPaymentMethod("EFT");
                paymentRequest.setPaymentReason(claim.getType().getCode() + "-CLAIM");
//                paymentRequest.setReference(claim.getMember().getIdentity().getNumber() + "-" + claim.getMember().getName1() + " " + claim.getMember().getName2());
                paymentRequest.setReference("FUNERAL" + claim.getNumber());
                paymentRequest.setDueDate(new Date());
                paymentRequest.setRecipientId(getFuneralServiceProvider());
                paymentRequest.setAmount(new BigDecimal(getAmount(claim.getMembership().getProduct().getId(), "FUNERAL-VALUE").getValue()));
                paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                String paymentRequestId = paymentRequestService.create(paymentRequest);
                List<BankAccountDto> bankAccountDtoList = bankAccountService.getList(getFuneralServiceProvider());
                if (bankAccountDtoList.iterator().hasNext()) {
                    BankAccountDto bankAccountDto = bankAccountDtoList.iterator().next();
                    BankAccountCreateDto bankAccountCreateDto = new BankAccountCreateDto();
                    bankAccountCreateDto.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccountCreateDto.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccountCreateDto.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccountCreateDto.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccountCreateDto.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccountCreateDto.setObjectId(paymentRequestId);
//                    paymentRequest.setBankAccount(bankAccountCreateDto);
                    bankAccountService.add(bankAccountCreateDto);
                }

                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }

                paymentRequest = new PaymentRequestCreateDto();
                if(claim.getPaymentMethod().getCode().equalsIgnoreCase("CASH")){
                    paymentRequest.setPaymentMethod("CASH");
                }
                paymentRequest.setPaymentReason("GROCERY-CLAIM");
                paymentRequest.setReference(getCompanyName());
                paymentRequest.setDueDate(new Date());
                paymentRequest.setRecipientId(claim.getMember().getId());
                paymentRequest.setAmount(new BigDecimal(getAmount(claim.getMembership().getProduct().getId(), "GROCERY-VALUE").getValue()));
                if(claim.getBranch().getCode() != null && !claim.getBranch().getCode().isEmpty()){
                    paymentRequest.setBranch(claim.getBranch().getCode());
                }
                paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                paymentRequestId = paymentRequestService.create(paymentRequest);
                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }
                if (claim.getPaymentMethod().getCode().equals("EFT")) {
                    BankAccountDto bankAccountDto = bankAccountService.getList(claimId).iterator().next();
                    BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                    bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccount.setObjectId(paymentRequestId);
                    bankAccountService.add(bankAccount);
//                    paymentRequest.setBankAccount(bankAccount);
                }

            }

            if (claim.getType().getCode().equals("TOMBSTONE")) {
                PaymentRequestCreateDto paymentRequest = new PaymentRequestCreateDto();
                paymentRequest.setPaymentMethod("EFT");
                paymentRequest.setPaymentReason(claim.getType().getCode() + "-CLAIM");
                paymentRequest.setReference(claim.getMember().getIdentity().getNumber() + "-" + claim.getMember().getName1() + "-" + claim.getMember().getName2());
                paymentRequest.setDueDate(new Date());
                paymentRequest.setRecipientId(getTombstoneServiceProvider());
                paymentRequest.setAmount(new BigDecimal(getAmount(claim.getMembership().getProduct().getId(), "TOMBSTONE-VALUE").getValue()));
                paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                String paymentRequestId = paymentRequestService.create(paymentRequest);
                List<BankAccountDto> bankAccountDtoList = bankAccountService.getList(getTombstoneServiceProvider());
                if (bankAccountDtoList.iterator().hasNext()) {
                    BankAccountDto bankAccountDto = bankAccountDtoList.iterator().next();
                    BankAccountCreateDto bankAccountCreateDto = new BankAccountCreateDto();
                    bankAccountCreateDto.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccountCreateDto.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccountCreateDto.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccountCreateDto.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccountCreateDto.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccountCreateDto.setObjectId(paymentRequestId);
                    bankAccountService.add(bankAccountCreateDto);
//                    paymentRequest.setBankAccount(bankAccountCreateDto);
                }

                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }

            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    private String getFuneralServiceProvider() {
        Properties properties = settingService.getSettings("FUNERAL-CLAIM");
        try {
            return properties.get("SERVICE-PROVIDER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getTombstoneServiceProvider() {
        Properties properties = settingService.getSettings("TOMBSTONE-CLAIM");
        try {
            return properties.get("SERVICE-PROVIDER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String getCompanyName() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("COMPANY-NAME").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private ProductAttributeDto getAmount(String product, String attribute) {
        ProductAttributeQueryDto productAttributeQueryDto = new ProductAttributeQueryDto();
        productAttributeQueryDto.setProduct(product);
        productAttributeQueryDto.setAttribute(attribute);
        ProductAttributeDto productAttributeDto = productService.getAttribute(productAttributeQueryDto);
        return productAttributeDto;
    }

}
