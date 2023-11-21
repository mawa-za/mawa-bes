package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
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

    public GroupSocietyDto get(String id) {
        GroupSocietyDto groupSocietyDto = null;
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (!transactionDto.equals(null)) {
                groupSocietyDto.setNumber(transactionDto.getNumber());
                groupSocietyDto.setId(transactionDto.getId());
                if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                    String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                    ProductDto productDto = productService.getOptionalById(productId);
                    if (productDto != null) {
                        transactionDto.setProductDetails(productDto);
                    }
                }
            }
            return groupSocietyDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

}
