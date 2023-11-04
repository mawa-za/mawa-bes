package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class MembershipService implements MembershipDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;

    @Override
    public MembershipDto create(MembershipCreateDto membershipCreateDto) throws PartnerNotFoundException, ProductNotFoundException, TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {

        if (partnerService.get(membershipCreateDto.getMemberId()) == null) {
            throw new PartnerNotFoundException("Membership main member does not exist");
        }
        if (productService.get(membershipCreateDto.getProductId()) == null) {
            throw new ProductNotFoundException("Membership product does not exist");
        }
        if (partnerService.get(membershipCreateDto.getSalesRepresentativeId()) == null) {
            throw new PartnerNotFoundException("Membership Sales Representative does not exist");
        }

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.MEMBERSHIP);
        transactionCreateDto.setSubType(membershipCreateDto.getMembershipType());
        if (Objects.equals(membershipCreateDto.getCreationType(), "TRANSFER")) {
            transactionCreateDto.setStatus(Status.PENDING);
            transactionCreateDto.setStatusReason(StatusReason.DOCUMENT_VERIFICATION);
        } else {
            transactionCreateDto.setStatus(Status.NEW);
        }
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        ProductDto productDto = productService.get(membershipCreateDto.getProductId());
        TransactionItemDto transactionItemDto = new TransactionItemDto();
        transactionItemDto.setTransaction(transactionDto.getId());
        transactionItemDto.setProduct(membershipCreateDto.getProductId());
        transactionItemDto.setProduct(productDto.getId());
        try {
            ProductPricingQueryDto productPricingQueryDto = new ProductPricingQueryDto();
            productPricingQueryDto.setProduct(membershipCreateDto.getProductId());
            productPricingQueryDto.setPricing(ProductPricing.MONTHLY_PREMIUM);
            ProductPricingDto productPricingDto = productService.getPricing(productPricingQueryDto);
            transactionItemDto.setUnitPrice(productPricingDto.getValue());
        } catch (Exception exception) {

        }
        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
        transactionItemDto.setQuantity(new BigDecimal("1"));
        transactionService.addItem(transactionItemDto);

        if (membershipCreateDto.getDateJoined() != null) {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(membershipCreateDto.getDateJoined());
            transactionService.addDate(dateJoined);
        } else {
            TransactionDateDto dateJoined = new TransactionDateDto();
            dateJoined.setTransaction(transactionDto.getId());
            dateJoined.setType(DateType.JOINED);
            dateJoined.setValue(new Date());
            transactionService.addDate(dateJoined);
        }
        if (membershipCreateDto.getLastReceiptDate() != null) {
            TransactionDateDto lastReceiptDate = new TransactionDateDto();
            lastReceiptDate.setTransaction(transactionDto.getId());
            lastReceiptDate.setType(DateType.LAST_RECEIPT_DATE);
            lastReceiptDate.setValue(membershipCreateDto.getLastReceiptDate());
            transactionService.addDate(lastReceiptDate);
        }

        if (Objects.equals(membershipCreateDto.getCreationType(), "TRANSFER")) {
            TransactionDateDto dateEffective = new TransactionDateDto();
            dateEffective.setTransaction(transactionDto.getId());
            dateEffective.setType(DateType.EFFECTIVE);
            dateEffective.setValue(new Date());
            transactionService.addDate(dateEffective);
        } else {
            TransactionDateDto dateEffective = new TransactionDateDto();
            dateEffective.setTransaction(transactionDto.getId());
            ProductAttributeQueryDto productAttributeQueryDto = new ProductAttributeQueryDto();
            productAttributeQueryDto.setProduct(membershipCreateDto.getProductId());
            productAttributeQueryDto.setAttribute(ProductAttribute.WAITING_PERIOD);
//            ProductAttributeDto productAttributeDto = productService.getAttribute(productAttributeQueryDto);
            ProductAttributeDto productAttributeDto = null;
            int waitingPeriod = 0;
            if (productAttributeDto != null) {
                waitingPeriod = Integer.parseInt(productAttributeDto.getValue());
            }
            dateEffective.setType(DateType.EFFECTIVE);
            dateEffective.setValue(Conversion.addMonthsToDate(new Date(), waitingPeriod));
            transactionService.addDate(dateEffective);
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

        if (membershipCreateDto.getPreviousInsurerId() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.PREVIOUS_INSURER);
            transactionPartnerDto.setPartner(membershipCreateDto.getPreviousInsurerId());
            transactionService.addPartner(transactionPartnerDto);
        }
        MembershipDto membershipDto = new MembershipDto();
        membershipDto.setId(transactionDto.getId());
        return membershipDto;

    }

    @Override
    public MembershipDto get(String id) {
        MembershipDto membershipDto = null;
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (!transactionDto.equals(null)) {
                membershipDto.setNumber(transactionDto.getNumber());
                membershipDto.setId(transactionDto.getId());
                if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                    String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                    ProductDto productDto = productService.getOptionalById(productId);
                    if (productDto != null) {
                        transactionDto.setProductDetails(productDto);
                    }
                }
            }
            return membershipDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<MembershipQueryResultDto> search(MembershipQueryDto membershipQueryDto) {
        return null;
    }

    @Override
    public void edit(MembershipEditDto membershipEditDto) {

    }

    @Override
    public void addDependent(DependentDto dependentDto) {

    }

    @Override
    public void removeDependent(DependentDto dependentDto) {

    }


}
