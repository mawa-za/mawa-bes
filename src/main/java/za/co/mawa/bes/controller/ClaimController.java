package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.*;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.xero.XeroAccountingService;
import za.co.mawa.bes.xero.XeroUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "claim")
public class ClaimController {
    private static final Logger logger = LoggerFactory.getLogger(ClaimController.class);

    @Autowired
    ClaimService claimService;
    @Autowired
    PartnerService partnerService;
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
    XeroAccountingService xeroAccountingService;

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
                paymentRequest.setRecipientId(claim.getClaimant().getId());
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
                PaymentRequestDto paymentRequestDto = paymentRequestService.create(paymentRequest);
                String paymentRequestId = paymentRequestDto.getId();

                try {
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
                } catch (Exception e) {

                }
                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionLinkType.CLAIM_PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }
            }

            if (claim.getType().getCode().equals("FUNERAL")) {
                PaymentRequestCreateDto funeralPaymentRequest = new PaymentRequestCreateDto();
                funeralPaymentRequest.setPaymentMethod("EFT");
                funeralPaymentRequest.setPaymentReason("FUNERAL-CLAIM");
                // Set reference from Xero invoice or claim number
                String itemCode = getProductItemCode(claim.getMembership().getProduct().getId());
                logger.info("Fetching xeroInvoiceCode");
                String xeroInvoice = xeroAccountingService.createInvoice(
                        getFuneralServiceProvider(),
                        partnerService.getFullName(claim.getDeceased()),
                        itemCode
                );
                logger.info("Done fetching xeroInvoiceCode");
                funeralPaymentRequest.setReference(xeroInvoice != null ? xeroInvoice : claim.getNumber());

                // setting common payment details
                logger.info("Setting common payment details ");
                funeralPaymentRequest.setDueDate(new Date());
                funeralPaymentRequest.setRecipientId(getFuneralServiceProvider());
                funeralPaymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());

                try {
                    funeralPaymentRequest.setAmount(getProductAmount(claim.getMembership().getProduct().getId(), "FUNERAL-VALUE"));
                    logger.info("Done retrieving the productAmount ");
                } catch (Exception ex) {
                    funeralPaymentRequest.setAmount(new BigDecimal("0"));
                }

                funeralPaymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                PaymentRequestDto paymentRequestDto = paymentRequestService.create(funeralPaymentRequest);
                String paymentRequestId = paymentRequestDto.getId();

                try {
                    if (funeralPaymentRequest.getPaymentMethod().equals("EFT")) {
                        BankAccountDto bankAccountDto = bankAccountService.getList(getFuneralServiceProvider()).iterator().next();
                        BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                        bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                        bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                        bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                        bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                        bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                        bankAccount.setObjectId(paymentRequestId);
                        bankAccountService.add(bankAccount);
                    }
                } catch (Exception e) {

                }
                if (paymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(paymentRequestId);
                    transactionLinkDto.setType(TransactionLinkType.CLAIM_PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(paymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);
                }

                PaymentRequestCreateDto groceryPaymentRequest = new PaymentRequestCreateDto();
                try {
                    groceryPaymentRequest.setPaymentMethod(claim.getPaymentMethod().getCode());
                } catch (Exception ex) {
                    groceryPaymentRequest.setPaymentMethod("CASH");
                }

                groceryPaymentRequest.setPaymentReason("GROCERY-CLAIM");
                groceryPaymentRequest.setReference("GROCERY" + (claim.getNumber() != null ? claim.getNumber() : ""));
                groceryPaymentRequest.setDueDate(new Date());
                groceryPaymentRequest.setRecipientId(claim.getClaimant().getId());

                try {
                    groceryPaymentRequest.setAmount(getProductAmount(claim.getMembership().getProduct().getId(), "GROCERY-VALUE"));
                    logger.info("Done retrieving the productAmount ");
                } catch (Exception ex) {
                    groceryPaymentRequest.setAmount(new BigDecimal("0"));
                }

                try {
                    groceryPaymentRequest.setBranch(getDefaultBranch());
                } catch (Exception e) {
                    logger.info("Default Grocery Claim Collection Branch not maintained");
                }

                groceryPaymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
                PaymentRequestDto groceryPaymentRequestDto = paymentRequestService.create(groceryPaymentRequest);
                String groceryPaymentRequestId = groceryPaymentRequestDto.getId();

                try {
                    if (groceryPaymentRequest.getPaymentMethod().equals("EFT")) {
                        BankAccountDto bankAccountDto = bankAccountService.getList(claimId).iterator().next();
                        BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                        bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                        bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                        bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                        bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                        bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                        bankAccount.setObjectId(groceryPaymentRequestId);
                        bankAccountService.add(bankAccount);
                    }
                } catch (Exception e) {

                }
                if (groceryPaymentRequestId != null) {
                    TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
                    transactionLinkDto.setTransaction1(claimId);
                    transactionLinkDto.setTransaction2(groceryPaymentRequestId);
                    transactionLinkDto.setType(TransactionLinkType.CLAIM_PAYMENT_REQUEST);
                    transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
                    transactionService.addLink(transactionLinkDto);

                    TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
                    transactionProcessDto.setId(groceryPaymentRequestId);
                    paymentRequestService.approve(transactionProcessDto);

                    TransactionEditDto transactionEditDto = new TransactionEditDto();
                    transactionEditDto.setId(claimId);
                    transactionEditDto.setStatus(Status.PROCESSED);
                    transactionService.edit(transactionEditDto);
                }

            }

            if (claim.getType().getCode().equals("TOMBSTONE")) {
                createTombstonePaymentRequest(claim);
            }
            if (claim.getType().getCode().equals("GROUP-FUNERAL")) {
                TransactionEditDto transactionEditDto = new TransactionEditDto();
                transactionEditDto.setId(claimId);
                transactionEditDto.setStatus(Status.PROCESSED);
                transactionService.edit(transactionEditDto);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/pdf-form", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generatePdf(@PathVariable String id) {
        try {
            ClaimOutboundDto claimOutboundDto = claimService.get(id);
            ByteArrayResource pdfResource = claimService.generateClaimPdf(claimOutboundDto);

            String base64Pdf = Base64.getEncoder().encodeToString(pdfResource.getByteArray());

            return ResponseEntity.ok().body(base64Pdf);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
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

    private String getDefaultBranch() {
        Properties properties = settingService.getSettings("GROCERY-CLAIM");
        try {
            return properties.get("DEFAULT-BRANCH").toString();
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

    private void createTombstonePaymentRequest(ClaimOutboundDto claim) throws Exception {
        PaymentRequestCreateDto tombstonePaymentRequest = new PaymentRequestCreateDto();
        tombstonePaymentRequest.setPaymentMethod("EFT");
        tombstonePaymentRequest.setPaymentReason("TOMBSTONE-CLAIM");
        tombstonePaymentRequest.setReference("TOMBSTONE" + claim.getNumber());
        tombstonePaymentRequest.setDueDate(new Date());
        tombstonePaymentRequest.setRecipientId(claim.getClaimant().getId());
        tombstonePaymentRequest.setAmount(new BigDecimal(getAmount(claim.getMembership().getProduct().getId(), "TOMBSTONE-VALUE").getValue()));
        tombstonePaymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
        PaymentRequestDto groceryPaymentRequestDto = paymentRequestService.create(tombstonePaymentRequest);
        String tombstonePaymentRequestId = groceryPaymentRequestDto.getId();
        try {
            List<BankAccountDto> bankAccountDtoList = bankAccountService.getList(getTombstoneServiceProvider());
            if (bankAccountDtoList.iterator().hasNext()) {
                BankAccountDto bankAccountDto = bankAccountDtoList.iterator().next();
                BankAccountCreateDto bankAccountCreateDto = new BankAccountCreateDto();
                bankAccountCreateDto.setAccountHolder(bankAccountDto.getAccountHolder());
                bankAccountCreateDto.setAccountType(bankAccountDto.getAccountType().getCode());
                bankAccountCreateDto.setBankName(bankAccountDto.getBankName().getCode());
                bankAccountCreateDto.setAccountNumber(bankAccountDto.getAccountNumber());
                bankAccountCreateDto.setBranchCode(bankAccountDto.getBranchCode());
                bankAccountCreateDto.setObjectId(tombstonePaymentRequestId);
                bankAccountService.add(bankAccountCreateDto);
//          paymentRequest.setBankAccount(bankAccountCreateDto);
            }
        } catch (Exception e) {

        }

        if (tombstonePaymentRequestId != null) {
            TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
            transactionLinkDto.setTransaction1(claim.getId());
            transactionLinkDto.setTransaction2(tombstonePaymentRequestId);
            transactionLinkDto.setType(TransactionType.PAYMENT_REQUEST);
            transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
            transactionService.addLink(transactionLinkDto);

            TransactionProcessDto transactionProcessDto = new TransactionProcessDto();
            transactionProcessDto.setId(tombstonePaymentRequestId);
            paymentRequestService.approve(transactionProcessDto);
        }
    }

    private void processFuneralClaim(ClaimOutboundDto claim) throws Exception {
        // 1. Processing main funeral payment request
        logger.info("Processing FUNERAL claim {}", claim.getNumber());
        PaymentRequestDto mainPaymentRequest = createFuneralPaymentRequest(claim);
        processPaymentRequest(claim, mainPaymentRequest);


        // 2. Processing grocery payment request
        logger.info("Creating grocery payment request ");
        PaymentRequestDto groceryPaymentRequest = createGroceryPaymentRequest(claim);
        logger.info("Done creating grocery payment request ");
        logger.info("Processing grocery payment request ");
        processPaymentRequest(claim, groceryPaymentRequest);
        logger.info("Done processing grocery payment request ");

        // 3. Processing tombstone recipients if needed
        processTombstoneRecipients(claim);
    }

    private PaymentRequestDto createFuneralPaymentRequest(ClaimOutboundDto claim) throws Exception {
        PaymentRequestCreateDto paymentRequest = new PaymentRequestCreateDto();
        paymentRequest.setPaymentMethod("EFT");
        paymentRequest.setPaymentReason("FUNERAL-CLAIM");
        // Set reference from Xero invoice or claim number
        String itemCode = getProductItemCode(claim.getMembership().getProduct().getId());
        logger.info("Fetching xeroInvoiceCode");
        String xeroInvoice = xeroAccountingService.createInvoice(
                getFuneralServiceProvider(),
                partnerService.getFullName(claim.getDeceased()),
                itemCode
        );
        logger.info("Done fetching xeroInvoiceCode");
        paymentRequest.setReference(xeroInvoice != null ? xeroInvoice : claim.getNumber());

        // setting common payment details
        logger.info("Setting common payment details ");
        paymentRequest.setDueDate(new Date());
        paymentRequest.setRecipientId(getFuneralServiceProvider());
        paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
        logger.info("Retrieving the productAmount ");
        paymentRequest.setAmount(getProductAmount(claim.getMembership().getProduct().getId(), "FUNERAL-VALUE"));
        logger.info("Done retrieving the productAmount ");
        logger.info("Creating Funeral Payment Request ");
        return paymentRequestService.create(paymentRequest);
    }

    private PaymentRequestDto createGroceryPaymentRequest(ClaimOutboundDto claim) throws Exception {
        try {
            Objects.requireNonNull(claim, "Claim cannot be null");

            PaymentRequestCreateDto paymentRequest = new PaymentRequestCreateDto();

            // 1. Handle payment method - default to EFT if null
            String paymentMethod = "EFT";
            if (claim.getPaymentMethod() != null && claim.getPaymentMethod().getCode() != null) {
                paymentMethod = "CASH".equalsIgnoreCase(claim.getPaymentMethod().getCode()) ? "CASH" : "EFT";
            }
            paymentRequest.setPaymentMethod(paymentMethod);

            // 2. Handle branch - only for CASH payments
            if ("CASH".equals(paymentMethod)) {
                String branchCode = "MODJADJISKLOOF"; // Default branch
                if (claim.getBranch() != null && claim.getBranch().getCode() != null) {
                    branchCode = claim.getBranch().getCode();
                }
                paymentRequest.setBranch(branchCode);
            }

            // 3. Set required fields with null checks
            paymentRequest.setPaymentReason("GROCERY-CLAIM");
            paymentRequest.setReference("GROCERY" + (claim.getNumber() != null ? claim.getNumber() : ""));
            paymentRequest.setDueDate(new Date());

            if (claim.getClaimant() != null && claim.getClaimant().getId() != null) {
                paymentRequest.setRecipientId(claim.getClaimant().getId());
            } else {
                throw new IllegalArgumentException("Claimant ID cannot be null");
            }

            paymentRequest.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());

            // 4. Handle amount with multiple fallbacks
            BigDecimal amount = BigDecimal.ZERO;
            try {
                if (claim.getMembership() != null
                        && claim.getMembership().getProduct() != null
                        && claim.getMembership().getProduct().getId() != null) {
                    amount = getProductAmount(claim.getMembership().getProduct().getId(), "GROCERY-VALUE");
                }
            } catch (Exception e) {
                logger.error("Failed to get GROCERY-VALUE amount", e);
            }
            paymentRequest.setAmount(amount);

            // 5. Create and return payment request
            PaymentRequestDto result = paymentRequestService.create(paymentRequest);
            if (result == null) {
                throw new IllegalStateException("Payment request creation returned null");
            }
            return result;

        } catch (Exception e) {
            logger.error("Failed to create grocery payment request for claim {}",
                    claim != null ? claim.getNumber() : "null", e);
            throw e;
        }
    }

    private String getProductItemCode(String productId) {
        return productService.getAttributes(productId).stream()
                .filter(attr -> XeroUtils.XERO_ITEM_CODE.equals(attr.getAttribute().getCode()))
                .findFirst()
                .map(ProductAttributeDto::getValue)
                .orElse(null);
    }

    private BigDecimal getProductAmount(String productId, String amountType) {
        try {
            return new BigDecimal(getAmount(productId, amountType).getValue());
        } catch (Exception e) {
            logger.error("Failed to get {} amount", amountType, e);
            return BigDecimal.ZERO;
        }
    }

    private void processPaymentRequest(ClaimOutboundDto claim, PaymentRequestDto paymentRequestDto) throws Exception {
        if (paymentRequestDto == null || paymentRequestDto.getId() == null) {
            logger.error("Failed to create payment request for claim {}", claim.getNumber());
            throw new RuntimeException("Payment request creation failed");
        }

        String paymentRequestId = paymentRequestDto.getId();
        logger.info("Created payment request with ID: {}", paymentRequestId);

        // Create transaction link
        TransactionLinkDto link = new TransactionLinkDto();
        link.setTransaction1(claim.getId());
        link.setTransaction2(paymentRequestId);
        link.setType(TransactionType.PAYMENT_REQUEST);
        link.setCreateBy(UserContext.getCurrentUserPartner());
        transactionService.addLink(link);

        // Approve the request
        TransactionProcessDto processDto = new TransactionProcessDto();
        processDto.setId(paymentRequestId);
        paymentRequestService.approve(processDto);

        // Handle EFT bank account if needed
        try {
            if ("EFT".equals(claim.getPaymentMethod().getCode())) {
                BankAccountDto bankAccountDto = bankAccountService.getList(claim.getId()).iterator().next();
                BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                bankAccount.setObjectId(paymentRequestId);
                bankAccountService.add(bankAccount);
            }
        } catch (Exception e) {
            logger.error("Error adding banking details {}", e.getMessage());
        }
    }

    private void processTombstoneRecipients(ClaimOutboundDto claim) {
        List<TombstoneRecipientDto> recipients = transactionService.getPartners(claim.getMembership().getId()).stream()
                .filter(p -> Objects.equals(p.getFunction(), PartnerFunction.TOMBSTONE_RECIPIENT))
                .map(p -> {
                    try {
                        return partnerService.get(p.getPartner());
                    } catch (PartnerNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .map(this::convertToTombstoneRecipient)
                .toList();

        recipients.stream()
                .filter(r -> claim.getDeceased().getId().equalsIgnoreCase(r.getId()))
                .findFirst()
                .ifPresent(r -> {
                    try {
                        createTombstonePaymentRequest(claim);
                    } catch (Exception e) {
                        logger.error("Failed to create tombstone payment", e);
                    }
                });
    }

    private TombstoneRecipientDto convertToTombstoneRecipient(PartnerDto partner) {
        TombstoneRecipientDto dto = new TombstoneRecipientDto();
        dto.setId(partner.getId());
        dto.setFirstName(partner.getName2());
        dto.setMiddleName(partner.getName3());
        dto.setLastName(partner.getName1());
        dto.setGender(partner.getGender().getDescription());
        dto.setTitle(partner.getTitle().getDescription());
        return dto;
    }
}
