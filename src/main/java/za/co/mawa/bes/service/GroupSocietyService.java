package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
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

    public GroupSocietyDto create(GroupSocietyCreateDto groupSocietyCreateDto) throws PartnerNotFoundException, ProductNotFoundException,
            TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.GROUP_SOCIETY);
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
        List<TransactionQueryResultDto> transactionQueryResultDtoList = transactionService.search(transactionQueryDto);
        for (TransactionQueryResultDto transactionQueryResultDto : transactionQueryResultDtoList) {
            groupSocietyDtoList.add(get(transactionQueryResultDto.getId()));
        }
        return groupSocietyDtoList;
    }

    public GroupSocietyDto get(String id) {
        try {
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
                } catch (PartnerNotFoundException e) {

                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equals(DateType.JOINED)) {
                    groupSocietyDto.setDateCreated(transactionDateDto.getValue());
                }
            }
            groupSocietyDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            groupSocietyDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            return groupSocietyDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }
    }
}
