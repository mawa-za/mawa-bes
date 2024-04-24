package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.dto.voucher.VoucherDto;
import za.co.mawa.bes.entity.transaction.TransactionAmountPKEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class GroupSocietyService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    ReceiptService receiptService;
    @Autowired
    ClaimService claimService;
    @Autowired
    VoucherService voucherService;

    public GroupSocietyDto create(GroupSocietyCreateDto groupSocietyCreateDto) throws PartnerNotFoundException, ProductNotFoundException,
            TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.GROUP_SOCIETY);
        transactionCreateDto.setLocation(groupSocietyCreateDto.getSalesArea());
        transactionCreateDto.setStatus(Status.ACTIVE);
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        ProductDto productDto = productService.get(groupSocietyCreateDto.getProduct());
        TransactionItemDto transactionItemDto = new TransactionItemDto();
        transactionItemDto.setTransaction(transactionDto.getId());
        transactionItemDto.setProduct(groupSocietyCreateDto.getProduct());
        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
        transactionItemDto.setQuantity(new BigDecimal("1"));
        transactionService.addItem(transactionItemDto);
        List<ProductPricingDto> productPricingDtoList = productDto.getPricings().stream()
                .filter(a -> Objects.equals(a.getPricing().getCode(), ProductPricing.SELLING_PRICE))
                .toList();
        if (productPricingDtoList.iterator().hasNext()) {
            TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(productPricingDtoList.iterator().next().getValue());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.SERVICE_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);
        }
        if (groupSocietyCreateDto.getCustomer() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
            transactionPartnerDto.setPartner(groupSocietyCreateDto.getCustomer());
            transactionService.addPartner(transactionPartnerDto);
        }
        if (groupSocietyCreateDto.getSalesRepresentative() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
            transactionPartnerDto.setPartner(groupSocietyCreateDto.getSalesRepresentative());
            transactionService.addPartner(transactionPartnerDto);
        }
        if (groupSocietyCreateDto.getDateJoined() != null) {
            TransactionDateDto transactionDateDto = new TransactionDateDto();
            transactionDateDto.setTransaction(transactionDto.getId());
            transactionDateDto.setType(DateType.JOINED);
            transactionDateDto.setValue(groupSocietyCreateDto.getDateJoined());
            transactionService.addDate(transactionDateDto);
        }
        TransactionDateDto transactionDateDto = new TransactionDateDto();
        transactionDateDto.setValue(new Date());
        transactionDateDto.setType(DateType.CREATED);
        transactionDateDto.setTransaction(transactionDto.getId());
        transactionService.addDate(transactionDateDto);

        TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
        transactionAmountInboundDto.setAmount(groupSocietyCreateDto.getOpeningBalance());
        transactionAmountInboundDto.setTransaction(transactionDto.getId());
        transactionAmountInboundDto.setType(AmountType.OPENING_BALANCE);
        transactionAmountService.save(transactionAmountInboundDto);
        return get(transactionDto.getId());

    }

    public List<GroupSocietyDto> search(GroupSocietyQueryDto groupSocietyQueryDto) {
        List<GroupSocietyDto> groupSocietyDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.GROUP_SOCIETY);
        for (String id : transactionService.search(transactionQueryDto)) {
            groupSocietyDtoList.add(get(id));
        }
        return groupSocietyDtoList;
    }

    public GroupSocietyDto get(String id) {
        try {
            calculateBalance(id);
            TransactionDto transactionDto = transactionService.get(id);
            GroupSocietyDto groupSocietyDto = new GroupSocietyDto();
            groupSocietyDto.setNumber(transactionDto.getNumber());
            groupSocietyDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
            groupSocietyDto.setId(transactionDto.getId());
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                try {
                    groupSocietyDto.setProduct(productService.getBasic(productId));
                } catch (ProductNotFoundException e) {
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.CUSTOMER)) {
                        groupSocietyDto.setCustomer(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                        groupSocietyDto.setSalesRepresentative(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                } catch (PartnerNotFoundException e) {

                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equals(DateType.JOINED)) {
                    groupSocietyDto.setDateJoined(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.CREATED)) {
                    groupSocietyDto.setDateCreated(transactionDateDto.getValue());
                }
            }
            groupSocietyDto.setAmounts(transactionAmountService.getByTransaction(id));
            groupSocietyDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            groupSocietyDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            groupSocietyDto.setSalesArea(fieldOptionService.getFieldOption(Field.SALES_AREA, transactionDto.getLocation()));

            return groupSocietyDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }
    }

    public void calculateBalance(String id) {
        try {
            BigDecimal totalDeposited = new BigDecimal("0.00");
            BigDecimal totalWithdrawn = new BigDecimal("0.00");
            BigDecimal openingBalance = new BigDecimal("0.00");
            ReceiptSearchDto receiptSearchDto = new ReceiptSearchDto();
            receiptSearchDto.setTransaction(id);
            for (ReceiptDto receiptDto : receiptService.getReceipts(receiptSearchDto)) {
                BigDecimal amount = receiptDto.getAmount();
                totalDeposited = totalDeposited.add(amount);
            }

//
//            for (VoucherDto voucherDto : voucherService.search(receiptSearchDto)) {
//                BigDecimal amount = receiptDto.getAmount();
//                totalDeposited = totalDeposited.add(amount);
//            }
            try {
                openingBalance = transactionAmountService.getByTransaction(id).stream()
                        .filter(a -> Objects.equals(a.getType().getCode(), AmountType.OPENING_BALANCE))
                        .toList().iterator().next().getAmount();
            } catch (Exception exception) {
            }
            BigDecimal availableBalance = (totalDeposited.subtract(totalWithdrawn)).add(openingBalance);
            try {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(availableBalance);
                transactionAmountInboundDto.setTransaction(id);
                transactionAmountInboundDto.setType(AmountType.AVAILABLE_BALANCE);
                transactionAmountService.save(transactionAmountInboundDto);
            } catch (Exception exception) {

            }
            try {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(totalDeposited);
                transactionAmountInboundDto.setTransaction(id);
                transactionAmountInboundDto.setType(AmountType.TOTAL_DEPOSITED);
                transactionAmountService.save(transactionAmountInboundDto);
            } catch (Exception exception) {

            }
            try {
                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                transactionAmountInboundDto.setAmount(totalWithdrawn);
                transactionAmountInboundDto.setTransaction(id);
                transactionAmountInboundDto.setType(AmountType.TOTAL_WITHDRAWN);
                transactionAmountService.save(transactionAmountInboundDto);
            } catch (Exception exception) {

            }
        } catch (Exception e) {

        }
    }
}
