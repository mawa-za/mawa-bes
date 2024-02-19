package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DependentDto;
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
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
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
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    ReceiptService receiptService;
    @Autowired
    ClaimService claimService;

    public GroupSocietyDto create(GroupSocietyCreateDto groupSocietyCreateDto) throws PartnerNotFoundException, ProductNotFoundException,
            TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.GROUP_SOCIETY);
        transactionCreateDto.setLocation(groupSocietyCreateDto.getSalesArea());
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        ProductDto productDto = productService.get(groupSocietyCreateDto.getProduct());
        TransactionItemDto transactionItemDto = new TransactionItemDto();
        transactionItemDto.setTransaction(transactionDto.getId());
        transactionItemDto.setProduct(groupSocietyCreateDto.getProduct());
        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
        transactionItemDto.setQuantity(new BigDecimal("1"));
        transactionService.addItem(transactionItemDto);
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

        TransactionAmountDto amountDto = new TransactionAmountDto();
        amountDto.setAmount(new BigDecimal(0));
        amountDto.setTransaction(transactionDto.getId());
        amountDto.setType(AmountType.AVAILABLE_BALANCE);
        transactionService.addAmount(amountDto);

        amountDto = new TransactionAmountDto();
        amountDto.setAmount(new BigDecimal(0));
        amountDto.setTransaction(transactionDto.getId());
        amountDto.setType(AmountType.TOTAL_DEPOSITED);
        transactionService.addAmount(amountDto);

        amountDto = new TransactionAmountDto();
        amountDto.setAmount(new BigDecimal(0));
        amountDto.setTransaction(transactionDto.getId());
        amountDto.setType(AmountType.TOTAL_WITHDRAWN);
        transactionService.addAmount(amountDto);

        GroupSocietyDto groupSocietyDto = new GroupSocietyDto();
        groupSocietyDto.setId(transactionDto.getId());
        return groupSocietyDto;

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
           // calculateBalance(id);
            TransactionDto transactionDto = transactionService.get(id);
            GroupSocietyDto groupSocietyDto = new GroupSocietyDto();
            groupSocietyDto.setNumber(transactionDto.getNumber());
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

            for (TransactionAmountDto transactionAmountDto : transactionService.getAmounts(id)) {
                AmountDto amountDto = new AmountDto();
                amountDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_AMOUNT, transactionAmountDto.getType()));
                amountDto.setAmount(transactionAmountDto.getAmount());
                groupSocietyDto.getAmounts().add(amountDto);
            }

            groupSocietyDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            groupSocietyDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            groupSocietyDto.setSalesArea(fieldOptionService.getFieldOption(Field.SALES_AREA, transactionDto.getLocation()));

            ReceiptSearchDto receiptSearchDto = new ReceiptSearchDto();
            receiptSearchDto.setTransaction(id);
            groupSocietyDto.setReceipts(receiptService.getReceipts(receiptSearchDto));

            ClaimQueryDto claimQueryDto = new ClaimQueryDto();
            claimQueryDto.setMembership(id);
            groupSocietyDto.setClaims(claimService.search(claimQueryDto));

            return groupSocietyDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }
    }

    public void calculateBalance(String id) {
        try {
            BigDecimal totalDeposited = new BigDecimal('0');
            BigDecimal totalWithdrawn = new BigDecimal('0');
            BigDecimal availableBalance = new BigDecimal('0');
            ReceiptSearchDto receiptSearchDto = new ReceiptSearchDto();
            receiptSearchDto.setTransaction(id);
            for (ReceiptDto receiptDto : receiptService.getReceipts(receiptSearchDto)) {
                totalDeposited.add(new BigDecimal(receiptDto.getAmount()));
            }
            try {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalDeposited.subtract(totalWithdrawn));
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.AVAILABLE_BALANCE);
                transactionService.editAmount(amountDto);
            } catch (Exception exception) {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalDeposited.subtract(totalWithdrawn));
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.AVAILABLE_BALANCE);
                transactionService.addAmount(amountDto);
            }

            try {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalDeposited);
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.TOTAL_DEPOSITED);
                transactionService.editAmount(amountDto);
            } catch (Exception exception) {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalDeposited.subtract(totalWithdrawn));
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.TOTAL_DEPOSITED);
                transactionService.addAmount(amountDto);
            }
            try {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalWithdrawn);
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.TOTAL_WITHDRAWN);
                transactionService.editAmount(amountDto);
            } catch (Exception exception) {
                TransactionAmountDto amountDto = new TransactionAmountDto();
                amountDto.setAmount(totalWithdrawn);
                amountDto.setTransaction(id);
                amountDto.setType(AmountType.TOTAL_WITHDRAWN);
                transactionService.addAmount(amountDto);
            }
        } catch (Exception e) {

        }
    }
}
