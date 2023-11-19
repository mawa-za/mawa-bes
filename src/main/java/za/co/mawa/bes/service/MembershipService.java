package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.MembershipDao;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
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

    public MembershipBasicDto getBasic(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            MembershipBasicDto membershipBasicDto = new MembershipBasicDto();
            membershipBasicDto.setNumber(transactionDto.getNumber());
            membershipBasicDto.setId(transactionDto.getId());
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                try {
                    membershipBasicDto.setProduct(productService.getBasic(productId));
                } catch (ProductNotFoundException e) {
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                        membershipBasicDto.setMember(partnerService.getBasic(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                        membershipBasicDto.setMember(partnerService.getBasic(transactionPartnerDto.getPartner()));
                    }
                } catch (PartnerNotFoundException e) {

                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (transactionDateDto.getType().equals(DateType.JOINED)) {
                    membershipBasicDto.setDateJoined(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.EFFECTIVE)) {
                    membershipBasicDto.setDateEffective(transactionDateDto.getValue());
                }
            }
            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionDto.getId());
            transactionAmountPKEntity.setType(TransactionAmount.MONTHLY_PREMIUM);
            membershipBasicDto.setPremium(transactionService.getAmount(transactionAmountPKEntity).getAmount());
            return membershipBasicDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    public MembershipDto getFull(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            MembershipDto membershipDto = new MembershipDto();
            membershipDto.setNumber(transactionDto.getNumber());
            membershipDto.setId(transactionDto.getId());
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                ProductDto productDto = productService.getOptionalById(productId);
                if (productDto != null) {
                    transactionDto.setProductDetails(productDto);
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                    membershipDto.setMemberId(transactionPartnerDto.getPartner());
                    PartnerDto partnerDto = partnerService.getOptional(transactionPartnerDto.getPartner());
                    if (partnerDto != null) {
                        PersonDto personDto = new PersonDto(partnerDto);
                        membershipDto.setMainMember(personDto);
                    }
                }
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                    membershipDto.setSalesRepresentativeId(transactionPartnerDto.getPartner());
                    PartnerDto partnerDto = partnerService.getOptional(transactionPartnerDto.getPartner());
                    if (partnerDto != null) {
                        PersonDto personDto = new PersonDto(partnerDto);
                        membershipDto.setSalesRep(personDto);
                    }
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
            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id).stream()
                    .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.DEPENDENT))
                    .toList();
            List<DependentDto> dependentDtoList = transactionPartnerDtoList.stream()
                    .map(TransactionPartnerDto::getPartner)
                    .map(partnerService::getOptional)
                    .filter(Objects::nonNull)
                    .map(partnerDto -> {
                        DependentDto dependentDto = new DependentDto();
                        dependentDto.setId(partnerDto.getId());
                        if (partnerDto.getTitle() != null) {
                            String title = fieldOptionService.getFieldOptionDescription(Field.TITLE, partnerDto.getTitle());
                            if (title != null) {
                                dependentDto.setTitle(title);
                            }
                        }
                        if (partnerDto.getGender() != null) {
                            String gender = fieldOptionService.getFieldOptionDescription(Field.GENDER, partnerDto.getGender());
                            if (gender != null) {
                                dependentDto.setGender(gender);
                            }
                        }
                        dependentDto.setIdType(partnerDto.getIdType());
                        dependentDto.setIdNumber(partnerDto.getIdNumber());
                        dependentDto.setLastName(partnerDto.getName1());
                        dependentDto.setFirstName(partnerDto.getName2());
                        dependentDto.setMiddleName(partnerDto.getName3());
                        return dependentDto;
                    })
                    .collect(Collectors.toList());
            membershipDto.setDependentDtoList(dependentDtoList);

            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionDto.getId());
            transactionAmountPKEntity.setType(TransactionAmount.MONTHLY_PREMIUM);
            membershipDto.setPremium(transactionService.getAmount(transactionAmountPKEntity).getAmount());
            return membershipDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    public MembershipDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            MembershipDto membershipDto = new MembershipDto();
            membershipDto.setNumber(transactionDto.getNumber());
            membershipDto.setId(transactionDto.getId());
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                ProductDto productDto = productService.getOptionalById(productId);
                if (productDto != null) {
                    membershipDto.setProductDetails(productDto);
                }
            }
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                    membershipDto.setMemberId(transactionPartnerDto.getPartner());
                    PartnerDto partnerDto = partnerService.getOptional(transactionPartnerDto.getPartner());
                    if (partnerDto != null) {
                        PersonDto personDto = new PersonDto(partnerDto);
                        membershipDto.setMainMember(personDto);
                    }
                }
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {
                    membershipDto.setSalesRepresentativeId(transactionPartnerDto.getPartner());
                    PartnerDto partnerDto = partnerService.getOptional(transactionPartnerDto.getPartner());
                    if (partnerDto != null) {
                        PersonDto personDto = new PersonDto(partnerDto);
                        membershipDto.setSalesRep(personDto);
                    }
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
            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id).stream()
                    .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.DEPENDENT))
                    .toList();
            List<DependentDto> dependentDtoList = transactionPartnerDtoList.stream()
                    .map(TransactionPartnerDto::getPartner)
                    .map(partnerService::getOptional)
                    .filter(Objects::nonNull)
                    .map(partnerDto -> {
                        DependentDto dependentDto = new DependentDto();
                        dependentDto.setId(partnerDto.getId());
                        if (partnerDto.getTitle() != null) {
                            String title = fieldOptionService.getFieldOptionDescription(Field.TITLE, partnerDto.getTitle());
                            if (title != null) {
                                dependentDto.setTitle(title);
                            }
                        }
                        if (partnerDto.getGender() != null) {
                            String gender = fieldOptionService.getFieldOptionDescription(Field.GENDER, partnerDto.getGender());
                            if (gender != null) {
                                dependentDto.setGender(gender);
                            }
                        }
                        dependentDto.setIdType(partnerDto.getIdType());
                        dependentDto.setIdNumber(partnerDto.getIdNumber());
                        dependentDto.setLastName(partnerDto.getName1());
                        dependentDto.setFirstName(partnerDto.getName2());
                        dependentDto.setMiddleName(partnerDto.getName3());
                        return dependentDto;
                    })
                    .collect(Collectors.toList());
            membershipDto.setDependentDtoList(dependentDtoList);

            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionDto.getId());
            transactionAmountPKEntity.setType(TransactionAmount.MONTHLY_PREMIUM);
            membershipDto.setPremium(transactionService.getAmount(transactionAmountPKEntity).getAmount());
            return membershipDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }

    }

    public List<MembershipBasicDto> search(MembershipQueryDto membershipQueryDto) {
        List<MembershipBasicDto> membershipQueryResultDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.MEMBERSHIP);
        List<TransactionQueryResultDto> transactionQueryResultDtoList = transactionService.search(transactionQueryDto);
        for (TransactionQueryResultDto transactionQueryResultDto : transactionQueryResultDtoList) {
            membershipQueryResultDtoList.add(getBasic(transactionQueryResultDto.getId()));
        }
        return membershipQueryResultDtoList;
    }

    public void edit(MembershipEditDto membershipEditDto) {

    }

    public void addDependent(DependentDto dependentDto) {

    }

    public void removeDependent(DependentDto dependentDto) {

    }


}
