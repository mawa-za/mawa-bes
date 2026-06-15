package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.premium.PremiumCreateDto;
import za.co.mawa.bes.dto.premium.PremiumDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.ReceiptNumberExist;
import za.co.mawa.bes.repository.PremiumRepository;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.PremiumService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import za.co.mawa.bes.dto.PremiumCreateRequestDto;
import za.co.mawa.bes.dto.PremiumResponseDto;
import za.co.mawa.bes.dto.PremiumUpdateRequestDto;
import za.co.mawa.bes.mapper.PremiumMapper;


@RestController
@CrossOrigin
@RequestMapping(value = "pay-app")
public class PayAppController {

    private final PremiumMapper premiumMapper;
    Gson gson = new Gson();
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    PremiumService premiumService;
    @Autowired
    PremiumRepository premiumRepository;
    @Autowired
    NumberRangeService numberRangeService;

    @RequestMapping(value = "membership", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMembers(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String mainPartner,
                                        @RequestParam(required = false) String employeeResponsibleName,
                                        @RequestParam(required = false) String creationDate,
                                        @RequestParam(required = false) String idNumber) {
        try {
            List<MembershipOutboundDto> membershipOutboundDtoList = new ArrayList<>();
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.MEMBERSHIP);
            if (status != null && status != "") {
                transactionViewDto.setStatus("ACTIVE");
            }
            for (TransactionViewEntity transactionViewEntity : transactionService.searchV2(transactionViewDto)) {
                MembershipOutboundDto membershipOutboundDto = new MembershipOutboundDto();
                membershipOutboundDto.setMemberId(transactionViewEntity.getTransactionId());
                membershipOutboundDto.setMembershipNo(transactionViewEntity.getTransactionNumber());
                membershipOutboundDto.setIdNumber(transactionViewEntity.getIdentityNumber());
                membershipOutboundDto.setFullName(transactionViewEntity.getMainPartner());
                membershipOutboundDto.setPlanId(transactionViewEntity.getProductId());
                membershipOutboundDto.setActive(true);
                membershipOutboundDto.setUpdatedAt(Conversion.dateTimeToString(new Date()));
                membershipOutboundDto.setPaidUpToPeriod(premiumService.determinePeriod(transactionViewEntity.getTransactionId()));
                membershipOutboundDtoList.add(membershipOutboundDto);
            }
            return ResponseEntity.ok(gson.toJson(membershipOutboundDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "plans", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProducts(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String type) {
        try {
            List<MembershipPlanOutboundDto> membershipPlanOutboundDtoList = new ArrayList<>();
            ProductQueryDto productQueryDto = new ProductQueryDto();
            productQueryDto.setCategory("MEMBERSHIP");
            productQueryDto.setType("MEMBERSHIP");

            for (ProductDto productDto : productService.search(productQueryDto)) {
                MembershipPlanOutboundDto membershipPlanOutboundDto = new MembershipPlanOutboundDto();
                membershipPlanOutboundDto.setPlanId(productDto.getId());
                membershipPlanOutboundDto.setName(productDto.getDescription());

                ProductPricingQueryDto productPricingQueryDto = new ProductPricingQueryDto();
                productPricingQueryDto.setProduct(productDto.getId());
                productPricingQueryDto.setPricing("MONTHLY-PREMIUM");
                try {
                    ProductPricingDto productPricingDto = productService.getPricing(productPricingQueryDto);
                    String amountStr = productPricingDto.getValue().toString().replace(".", "");
                    membershipPlanOutboundDto.setPremiumCents(amountStr);
                } catch (Exception e) {
                }
                membershipPlanOutboundDto.setActive(true);
                membershipPlanOutboundDtoList.add(membershipPlanOutboundDto);
            }
            return ResponseEntity.ok(gson.toJson(membershipPlanOutboundDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "receipt-sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> syncReceipt(@RequestBody PremiumInboundDto premiumInboundDto) throws RuntimeException {
        try {
            PremiumResponseDto entity = new PremiumEntity();
            entity.setId(premiumInboundDto.getDeviceReceiptId());
            if (!StringUtils.isBlank(premiumInboundDto.getReceiptNo())) {
                entity.setReceiptNumber(premiumInboundDto.getReceiptNo());
            } else {
                entity.setReceiptNumber(numberRangeService.generateNumber(NumberRangeType.RECEIPT));
            }
            entity.setMembershipId(premiumInboundDto.getMemberId());
            entity.setMembershipPeriod(premiumInboundDto.getPaymentPeriod());
//            entity.setLocation(premiumInboundDto.getLocation());
            entity.setTerminalId(premiumInboundDto.getDeviceId());

            String ts = premiumInboundDto.getCreatedAt();

            Instant instant = Instant.parse(ts);
            Date date = Date.from(instant);

            entity.setCreationDate(date);
            entity.setCreationTime(date);
            entity.setCreatedBy(premiumInboundDto.getUserId());
            entity.setTenderType(premiumInboundDto.getPaymentMethod().toUpperCase());
            BigDecimal amount = new BigDecimal(premiumInboundDto.getAmountCents()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            entity.setAmount(amount);
            PremiumResponseDto premiumEntity = premiumRepository.save(entity);
            premiumService.updatePaidUpToPeriod(premiumInboundDto.getMemberId());
            return ResponseEntity.ok(new ReceiptSyncResponse(
                    "SUCCESS",
                    premiumEntity.getId(),
                    premiumEntity.getReceiptNumber(),
                    null,
                    "Receipt created"
            ));

        }
        catch (Exception e) {

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    new ReceiptSyncResponse(
                            "RETRY",
                            null,
                            null,
                            "TEMP_ERROR",
                            "Temporary failure"
                    )
            );
        }

    }
}
