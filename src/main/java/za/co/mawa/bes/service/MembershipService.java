package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.membership.*;
import za.co.mawa.bes.dto.partner.PartnerQueryDto;
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
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MembershipService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;

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

//        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
//        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
//        transactionQueryDto.setSubtype(membershipCreateDto.getMembershipType());
//        List<String> transactionQueries = transactionService.search(transactionQueryDto);
//
//        if (!transactionQueries.isEmpty()) {
//            for (String transactionQuery : transactionQueries) {
//                TransactionPartnerDto transactionPartnerDto = transactionService.getPartner(transactionQuery, PartnerFunction.MAINMEMBER);
//                if (transactionPartnerDto != null) {
//                    if (transactionPartnerDto.getPartner().equals(membershipCreateDto.getMemberId())) {
//                        throw new TransactionPartnerAddException("Membership id with the same membership type already exist");
//                    }
//                }
//
//            }
//        }
//        TransactionPartnerDto transactionPartnerDto = transactionService.getPartner(,);
//        if(transactionService.getPartner(t,p))
//        {
//
//
//        }

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
        transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
        transactionItemDto.setQuantity(new BigDecimal("1"));
        transactionService.addItem(transactionItemDto);

        TransactionAmountDto transactionAmountDto = new TransactionAmountDto();
        transactionAmountDto.setTransaction(transactionDto.getId());
        transactionAmountDto.setType(TransactionAmount.MONTHLY_PREMIUM);
        transactionAmountDto.setAmount(transactionItemDto.getUnitPrice());
        transactionService.addAmount(transactionAmountDto);

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

    public MembershipDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            MembershipDto membershipDto = new MembershipDto();
            membershipDto.setNumber(transactionDto.getNumber());
            membershipDto.setId(transactionDto.getId());
            membershipDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                try {
                    membershipDto.setProduct(productService.getBasic(productId));
                } catch (ProductNotFoundException e) {
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                        membershipDto.setMember(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                        membershipDto.setSalesRepresentative(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                } catch (PartnerNotFoundException e) {

                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equals(DateType.JOINED)) {
                    membershipDto.setDateJoined(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.EFFECTIVE)) {
                    membershipDto.setDateEffective(transactionDateDto.getValue());
                }
            }
            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionDto.getId());
            transactionAmountPKEntity.setType(TransactionAmount.MONTHLY_PREMIUM);
            membershipDto.setPremium(transactionService.getAmount(transactionAmountPKEntity).getAmount());
            membershipDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            membershipDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            return membershipDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    public List<MembershipDto> search(MembershipQueryDto membershipQueryDto) {
        List<MembershipDto> membershipDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();

        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
        for (String id : transactionService.search(transactionQueryDto)) {
            membershipDtoList.add(get(id));
        }
        return membershipDtoList;
    }

    public void edit(MembershipEditDto membershipEditDto) {

    }

    public List<MembershipDto> getByFilter(MembershipQueryDto membershipQueryDto) throws Exception {
        List<MembershipDto> membershipDtoList = new ArrayList<>();
        List<String> transactionList = new ArrayList<>();
        if (membershipQueryDto.getPartnerFunction() != null || membershipQueryDto.getMemberId() != null) {

            List<TransactionPartnerDto> transactionPartnerDtos = transactionService.getPartnersByFunction(PartnerFunction.MAINMEMBER);

            if (!transactionPartnerDtos.isEmpty()) {
                for (TransactionPartnerDto transactionPartnerDto : transactionPartnerDtos) {
                    String transaction = "";

                    if (membershipQueryDto.getMemberId() != null) {
                        if (transactionPartnerDto.getPartner().equals(membershipQueryDto.getMemberId())) {
                            transaction = transactionPartnerDto.getTransaction();
                        }
                    } else {
                        transaction = transactionPartnerDto.getTransaction();
                    }

                    transactionList.add(transaction);

                }

            }

        }

        List<TransactionItemDto> transactionItemDtos = new ArrayList<>();
        if (!transactionList.isEmpty() && membershipQueryDto.getProductId() != null) {

            Iterator<String> iterator = transactionList.iterator();

            while (iterator.hasNext()) {
                String transaction = iterator.next();
                transactionItemDtos = transactionService.getItems(transaction);

                if (!transactionItemDtos.isEmpty()) {
                    for (TransactionItemDto transactionItemDto : transactionItemDtos) {
                        if (!transactionItemDto.getProduct().equals(membershipQueryDto.getProductId())) {
                            iterator.remove();
                        }
                    }
                }
            }

        } else if (membershipQueryDto.getProductId() != null) {

            Iterator<String> iterator = transactionList.iterator();
            TransactionItemDto transactionItemDto = new TransactionItemDto();
            transactionItemDto.setProduct(membershipQueryDto.getProductId());
            List<TransactionItemDto> transactionItemDtoList = transactionService.getItemsBy(transactionItemDto);


            if (!transactionItemDtoList.isEmpty()) {
                for (TransactionItemDto transactionItemDto1 : transactionItemDtoList) {
                    while (iterator.hasNext()) {
                        String transaction = iterator.next();
                        if (!transaction.equals(transactionItemDto1.getTransaction())) {
                            iterator.remove();
                        }

                    }

                }

            }
        }


        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();

        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
        for (String id : transactionService.search(transactionQueryDto)) {

            if (transactionList.isEmpty()) {
                MembershipDto membershipDto = get(id);
                if (membershipQueryDto.getStatus() != null) {
                    if (membershipDto.getStatus().getCode().equals(membershipQueryDto.getStatus())) {
                        membershipDtoList.add(membershipDto);
                    }

                } else {
                    membershipDtoList.add(membershipDto);
                }

            } else {
                Iterator<String> iterator = transactionList.iterator();
                while (iterator.hasNext()) {
                    String transaction = iterator.next();
                    if (transaction.equals(id)) {
                        MembershipDto membershipDto = get(transaction);

                        if (membershipQueryDto.getStatus() != null) {
                            if (membershipDto.getStatus().getCode().equals(membershipQueryDto.getStatus())) {
                                membershipDtoList.add(membershipDto);
                            }

                        } else {
                            membershipDtoList.add(membershipDto);
                        }
                    }
                }

            }

        }

        return membershipDtoList;
    }

}
