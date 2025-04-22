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
            logger.info("Processing payment request for claim ID: {}", claimId);

            ClaimOutboundDto claim = claimService.get(claimId);
            if (claim == null) {
                logger.error("Claim not found for ID: {}", claimId);
                return ResponseEntity.notFound().build();
            }

            switch (claim.getType().getCode()) {
                case "CASH":
                    processCashClaim(claim);
                    break;
                case "FUNERAL":
                    processFuneralClaim(claim);
                    break;
                case "TOMBSTONE":
                    createTombstonePaymentRequest(claim);
                    break;
                default:
                    logger.warn("Unknown claim type: {}", claim.getType().getCode());
            }

            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            logger.error("Error processing payment request", exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
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

    private ProductAttributeDto getAmount(String product, String attribute) {
        ProductAttributeQueryDto productAttributeQueryDto = new ProductAttributeQueryDto();
        productAttributeQueryDto.setProduct(product);
        productAttributeQueryDto.setAttribute(attribute);
        ProductAttributeDto productAttributeDto = productService.getAttribute(productAttributeQueryDto);
        return productAttributeDto;
    }

    private void processCashClaim(ClaimOutboundDto claim) throws Exception {
        logger.debug("Processing CASH claim {}", claim.getNumber());

        PaymentRequestCreateDto paymentRequest = buildBasePaymentRequest(claim);
        paymentRequest.setPaymentReason(claim.getType().getCode() + "-CLAIM");
        paymentRequest.setReference("CASHCLAIM" + claim.getNumber());

        setPaymentAmount(paymentRequest, claim.getPaidOutAmount().getAmount());
        setBranch(paymentRequest, claim.getBranch().getCode(), "MODJADJISKLOOF");

        PaymentRequestDto paymentRequestDto = paymentRequestService.create(paymentRequest);
        processPaymentRequest(claim, paymentRequestDto);
    }

    private void processFuneralClaim(ClaimOutboundDto claim) throws Exception {
        logger.debug("Processing FUNERAL claim {}", claim.getNumber());

        // Main funeral payment
        PaymentRequestCreateDto paymentRequest = buildBasePaymentRequest(claim);
        paymentRequest.setPaymentMethod("EFT");
        paymentRequest.setPaymentReason(claim.getType().getCode() + "-CLAIM");

        String xeroInvoiceNumber = generateXeroInvoice(claim);
        paymentRequest.setReference(xeroInvoiceNumber != null ? xeroInvoiceNumber : claim.getNumber());

        BigDecimal funeralAmount = getProductAmount(claim.getMembership().getProduct().getId(), "FUNERAL-VALUE");
        paymentRequest.setAmount(funeralAmount);

        PaymentRequestDto paymentRequestDto = paymentRequestService.create(paymentRequest);
        processPaymentRequest(claim, paymentRequestDto);

        // Grocery payment
        processGroceryPayment(claim);

        // Tombstone recipients
        processTombstoneRecipients(claim);
    }

    private String generateXeroInvoice(ClaimOutboundDto claim) {
        String itemCode = productService.getAttributes(claim.getMembership().getProduct().getId()).stream()
                .filter(attr -> attr.getAttribute().getCode().equals(XeroUtils.XERO_ITEM_CODE))
                .findFirst()
                .map(ProductAttributeDto::getValue)
                .orElse(null);

        return xeroAccountingService.createInvoice(
                getFuneralServiceProvider(),
                partnerService.getFullName(claim.getDeceased()),
                itemCode
        );
    }

    private void processGroceryPayment(ClaimOutboundDto claim) throws Exception {
        logger.debug("Processing GROCERY payment for claim {}", claim.getNumber());

        PaymentRequestCreateDto groceryPaymentRequest = buildBasePaymentRequest(claim);
        groceryPaymentRequest.setPaymentReason("GROCERY-CLAIM");
        groceryPaymentRequest.setReference("GROCERY" + claim.getNumber());

        if (claim.getPaymentMethod().getCode().equalsIgnoreCase("CASH")) {
            groceryPaymentRequest.setPaymentMethod("CASH");
            setBranch(groceryPaymentRequest, claim.getBranch().getCode(), "MODJADJISKLOOF");
        } else {
            groceryPaymentRequest.setPaymentMethod("EFT");
        }

        BigDecimal groceryAmount = getProductAmount(claim.getMembership().getProduct().getId(), "GROCERY-VALUE");
        groceryPaymentRequest.setAmount(groceryAmount);

        PaymentRequestDto groceryRequestDto = paymentRequestService.create(groceryPaymentRequest);
        processPaymentRequest(claim, groceryRequestDto);
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

    private PaymentRequestCreateDto buildBasePaymentRequest(ClaimOutboundDto claim) {
        PaymentRequestCreateDto request = new PaymentRequestCreateDto();
        request.setPaymentMethod(claim.getPaymentMethod().getCode());
        request.setDueDate(new Date());
        request.setRecipientId(claim.getClaimant().getId());
        request.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());
        return request;
    }

    private void setPaymentAmount(PaymentRequestCreateDto request, BigDecimal amount) {
        try {
            request.setAmount(amount);
        } catch (Exception ex) {
            logger.warn("Failed to set payment amount, defaulting to 0", ex);
            request.setAmount(BigDecimal.ZERO);
        }
    }

    private void setBranch(PaymentRequestCreateDto request, String branchCode, String defaultBranch) {
        try {
            request.setBranch(branchCode);
        } catch (Exception e) {
            logger.warn("Failed to set branch, using default: {}", defaultBranch, e);
            request.setBranch(defaultBranch);
        }
    }

    private BigDecimal getProductAmount(String productId, String amountType) {
        try {
            return new BigDecimal(getAmount(productId, amountType).getValue());
        } catch (Exception e) {
            logger.error("Failed to get {} amount for product {}", amountType, productId, e);
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
    }

    private void createTombstonePaymentRequest(ClaimOutboundDto claim) throws Exception {
        logger.debug("Creating TOMBSTONE payment for claim {}", claim.getNumber());

        PaymentRequestCreateDto request = new PaymentRequestCreateDto();
        request.setPaymentMethod("EFT");
        request.setPaymentReason("TOMBSTONE-CLAIM");
        request.setReference("TOMBSTONE" + claim.getNumber());
        request.setDueDate(new Date());
        request.setRecipientId(claim.getClaimant().getId());
        request.setAmount(getProductAmount(claim.getMembership().getProduct().getId(), "TOMBSTONE-VALUE"));
        request.setEmployeeResponsibleId(UserContext.getCurrentUserPartner());

        PaymentRequestDto paymentRequestDto = paymentRequestService.create(request);
        String paymentRequestId = paymentRequestDto.getId();

        // Add bank account
        bankAccountService.getList(getTombstoneServiceProvider()).stream()
                .findFirst()
                .ifPresent(bankAccountDto -> {
                    BankAccountCreateDto bankAccount = new BankAccountCreateDto();
                    bankAccount.setAccountHolder(bankAccountDto.getAccountHolder());
                    bankAccount.setAccountType(bankAccountDto.getAccountType().getCode());
                    bankAccount.setBankName(bankAccountDto.getBankName().getCode());
                    bankAccount.setAccountNumber(bankAccountDto.getAccountNumber());
                    bankAccount.setBranchCode(bankAccountDto.getBranchCode());
                    bankAccount.setObjectId(paymentRequestId);
                    bankAccountService.add(bankAccount);
                });

        // Process the payment request
        processPaymentRequest(claim, paymentRequestDto);
    }
}
