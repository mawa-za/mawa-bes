package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.*;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.dao.TransactionDao;

import java.util.*;

@Service
public class TransactionService implements TransactionDao {
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionItemRepository transactionItemRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    TransactionPartnerRepository transactionPartnerRepository;
    @Autowired
    TransactionAmountRepository transactionAmountRepository;
    @Autowired
    UserService userService;
    @Autowired
    PricingService pricingService;
    @Autowired
    ProductService productService;
    @Autowired
    TransactionLinkRepository transactionLinkRepository;
    @Autowired
    PartnerService partnerService;

    @Override
    public TransactionDto create(TransactionCreateDto transactionCreateDto) {
        try {
            TransactionEntity transactionEntity = new TransactionEntity(transactionCreateDto);
            String id = numberRangeService.generateNumber(transactionEntity.getType());
            transactionEntity.setNumber(id);
            if (transactionCreateDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setType(transactionCreateDto.getType());
            transactionEntity.setSubType(transactionCreateDto.getSubType());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(userService.getCurrentUser());
            TransactionEntity createdTransactionEntity = transactionRepository.save(transactionEntity);

            TransactionAmountDto totalIncVat = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.TOTAL_INC_VAT);
            addAmount(totalIncVat);
            TransactionAmountDto totalExcVat = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.TOTAL_EXC_VAT);
            addAmount(totalExcVat);
            TransactionAmountDto discountAmount = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.DISCOUNT_AMOUNT);
            addAmount(discountAmount);
            TransactionAmountDto discountPercentage = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.DISCOUNT_PERCENT);
            addAmount(discountPercentage);
            TransactionAmountDto VATAmount = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.VAT_AMOUNT);
            addAmount(VATAmount);
            TransactionAmountDto VATPercentage = new TransactionAmountDto(createdTransactionEntity.getId(), PriceType.VAT_PERCENT);
            addAmount(VATPercentage);
            return new TransactionDto(createdTransactionEntity);
        } catch (NumberRangeObjectNotFound ex) {
            throw new RuntimeException("Object number range not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void delete(String id) throws Exception {
        try {
            transactionRepository.deleteById(id);
        } catch (Exception ex) {
            throw new Exception("Error deleting transaction");
        }
    }

    @Override
    public void addDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDateEntity transactionDateEntity = new TransactionDateEntity(transactionDateDto);
        if (transactionDateDto.getValue() != null) {
            transactionDateEntity.setValue(transactionDateDto.getValue());
        } else {
            transactionDateEntity.setValue(new Date());
        }
        try {
            transactionDateRepository.save(transactionDateEntity);
        } catch (Exception ex) {
            throw new Exception("Error adding date to transaction");
        }
    }

    @Override
    public void removeDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
        transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        transactionDatePKEntity.setType(transactionDateDto.getType());
        try {
            transactionDateRepository.deleteById(transactionDatePKEntity);
        } catch (Exception ex) {
            throw new Exception("Date not added");
        }
    }

    @Override
    public List<TransactionDateDto> getDates(String id) {
        List<TransactionDateDto> transactionDateDtos = new ArrayList<>();
        List<TransactionDateEntity> transactionDateEntities = transactionDateRepository.getTransactionDates(id);
        for (TransactionDateEntity transactionDateEntity : transactionDateEntities) {
            transactionDateDtos.add(new TransactionDateDto(transactionDateEntity));
        }
        return transactionDateDtos;
    }

    @Override
    public void addAttachment(TransactionAttachmentDto transactionAttachmentDto) {

    }

    @Override
    public void removeAttachment(TransactionAttachmentDto transactionAttachmentDto) {

    }

    @Override
    public List<TransactionAttachmentDto> getAttachments(String id) {
        return null;
    }

    @Override
    public void addLink(TransactionLinkDto transactionLinkDto) throws Exception {

        TransactionLinkEntity transactionLinkEntity = new TransactionLinkEntity(transactionLinkDto);
        if (transactionLinkDto.getCreationDate() != null) {
            transactionLinkEntity.setCreation_date(transactionLinkDto.getCreationDate());
        } else {
            transactionLinkEntity.setCreation_date(new Date());
        }
        try {
            transactionLinkRepository.save(transactionLinkEntity);
        } catch (Exception ex) {
            throw new Exception("Error adding transaction link");
        }

    }

    @Override
    public void removeLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public List<TransactionLinkDto> getLinks(String id) {


        List<TransactionLinkDto> transactionLinkDtos = new ArrayList<>();
        List<TransactionLinkEntity> transactionLinkEntities = transactionLinkRepository.getTransactionLinks(id);

        for (TransactionLinkEntity transactionLinkEntity : transactionLinkEntities) {
            transactionLinkDtos.add(new TransactionLinkDto(transactionLinkEntity));
        }
        return transactionLinkDtos;
        
//        return null;
    }

    @Override
    public TransactionAmountDto getAmount(TransactionAmountPKEntity id) {

        Optional<TransactionAmountEntity> transactionAmountPKEntity = transactionAmountRepository.findById(id);
        TransactionAmountDto transactionAmountDto = new TransactionAmountDto();
        TransactionAmountEntity transactionAmountEntity = transactionAmountPKEntity.orElse(null);
        if (transactionAmountEntity != null) {
            transactionAmountDto = EntityToDto(transactionAmountPKEntity);

        }


        return transactionAmountDto;
    }

    private TransactionAmountDto EntityToDto(Optional<TransactionAmountEntity> transactionAmountEntity) {

        TransactionAmountDto transactionAmountDto = new TransactionAmountDto();
        transactionAmountDto.setTransaction(transactionAmountEntity.get().getTransactionAmountPKEntity().getTransaction());
        transactionAmountDto.setType(transactionAmountEntity.get().getTransactionAmountPKEntity().getType());
        transactionAmountDto.setAmount(transactionAmountEntity.get().getAmount());
        return transactionAmountDto;
    }

    @Override
    public List<TransactionPartnerDto> getPartners(String transactionId) {
        List<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto(transactionPartnerEntity);
            transactionPartnerDtos.add(transactionPartnerDto);
        }
        return transactionPartnerDtos;
    }

    @Override
    public TransactionPartnerDto getPartner(String transaction, String partnerFunction) {
        TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
        List<TransactionPartnerDto> transactionPartnerDtoList = getPartners(transaction).stream()
                .filter(a -> Objects.equals(a.getFunction(), partnerFunction))
                .toList();
        if (transactionPartnerDtoList.iterator().hasNext()) {
            return transactionPartnerDtoList.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public List<TransactionQueryResultDto> search(TransactionQueryDto transactionQueryDto) {
        List<String> transactionIdList = new ArrayList<>();
        List<TransactionQueryResultDto> transactionQueryResultDtoList = new ArrayList<>();
        if (transactionQueryDto.getPartnerFunction() != null && transactionQueryDto.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(transactionQueryDto.getPartnerNo());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
                if (transactionPartnerEntity.getTransactionPartnerPKEntity().getFunction().equals(transactionQueryDto.getPartnerFunction())) {
                    transactionIdList.add(transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction());
                }
            }
        }


        if (transactionQueryDto.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(transactionQueryDto.getType());
            for (TransactionEntity transactionEntity : transactions) {
                transactionIdList.add(transactionEntity.getId());
            }
        }

        if (transactionQueryDto.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(transactionQueryDto.getStatus());
            for (TransactionEntity transactionEntity : transactions) {
                transactionIdList.add(transactionEntity.getId());
            }
        }

        for (String transactionId : transactionIdList) {
            try {
                TransactionDto transactionDto = get(transactionId);
                TransactionQueryResultDto object = new TransactionQueryResultDto();
                object.setId(transactionDto.getId());
                object.setNumber(transactionDto.getNumber());
                object.setDescription(transactionDto.getDescription());
                object.setType(transactionDto.getType());
                object.setSubType(transactionDto.getSubType());
                object.setStatus(transactionDto.getStatus());

                List<TransactionDateDto> transactionDateDtoList = getDates(transactionId);

                MembershipDto membershipDto = new MembershipDto();


                for (TransactionPartnerDto transactionPartnerDto : getPartners(transactionId)) {

                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                        membershipDto.setMemberId(transactionPartnerDto.getPartner());


                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {

                        membershipDto.setSalesRepresentativeId(transactionPartnerDto.getPartner());

                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.CUSTOMER)) {
                        object.setCustomerId(transactionPartnerDto.getPartner());
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SUPPLIER)) {
                        object.setSupplierId(transactionPartnerDto.getPartner());
                    }
                }


                for (TransactionDateDto transactionDateDto : getDates(transactionId)) {
                    if (transactionDateDto.getType().equals(DateType.ORDER_DATE)) {
                        object.setOrderDate(transactionDateDto.getValue());
                    }
                    if (transactionQueryDto.getType().equals(TransactionType.MEMBERSHIP)) {
                        if (transactionDateDto.getType().equals(DateType.JOINED)) {
                            membershipDto.setDateJoined(transactionDateDto.getValue());
                        }
                        if (transactionDateDto.getType().equals(DateType.EFFECTIVE)) {
                            membershipDto.setDateEffective(transactionDateDto.getValue());
                        }
                    }


                    if (transactionDateDto.getType().equals(DateType.INVOICE_DATE)) {
                        object.setInvoiceDate(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equals(DateType.DELIVERY_DATE)) {
                        object.setDeliveryDate(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equals(DateType.EXPIRY_DATE)) {
                        object.setExpiryDate(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equals(DateType.DUE_DATE)) {
                        object.setDueDate(transactionDateDto.getValue());
                    }
                }

                if (transactionQueryDto.getType().equals(TransactionType.MEMBERSHIP)) {

                    membershipDto.setStatus(transactionDto.getStatus());
                    membershipDto.setStatusReason(transactionDto.getStatusReason());

                    List<TransactionItemDto> transactionItemDtoList = getItems(transactionId);


                    String productId = transactionItemDtoList.stream()
                            .map(TransactionItemDto::getProduct)
                            .findFirst()
                            .orElse(null);

                    if (productId != null) {
                        ProductDto productDto = productService.getOptionalById(productId);
                        if (productDto != null) {
                            membershipDto.setProductId(productDto.getId());
                            membershipDto.setProductDescription(productDto.getDescription());
                        }
                        TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
                        transactionAmountPKEntity.setTransaction(transactionId);
                        transactionAmountPKEntity.setType(PriceType.TOTAL_INC_VAT);
                        TransactionAmountDto transactionAmountDto = getAmount(transactionAmountPKEntity);
                        membershipDto.setPremium(transactionAmountDto.getAmount());
                    }


                    object.setMembershipHolder(membershipDto);
                }
                transactionQueryResultDtoList.add(object);
            } catch (TransactionNotFound exception) {
            }
        }
        return transactionQueryResultDtoList;
    }

    @Override
    public void edit(TransactionDto transactionDto) {

    }

    @Override
    public TransactionDto get(String transactionId) throws TransactionNotFound {
        TransactionEntity transactionEntity = transactionRepository.getById(transactionId);
        if (transactionEntity != null) {
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setId(transactionEntity.getId());
            transactionDto.setNumber(transactionEntity.getNumber());
            transactionDto.setDescription(transactionEntity.getDescription());
            transactionDto.setType(transactionEntity.getType());
            transactionDto.setSubType(transactionEntity.getSubType());
            transactionDto.setStatus(transactionEntity.getStatus());
            MembershipDto membershipDto = new MembershipDto();
            for (TransactionPartnerDto transactionPartnerDto : getPartners(transactionId)) {

                if (transactionEntity.getType().equals(TransactionType.MEMBERSHIP)) {

                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.MAINMEMBER)) {
                        membershipDto.setMemberId(transactionPartnerDto.getPartner());
                        PartnerDto partnerDto = partnerService.getOptional(transactionPartnerDto.getPartner());
                        if (partnerDto != null)
                        {
                            membershipDto.setMainMember(partnerDto);
                        }

                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SALES_REPRESENTATIVE)) {

                        membershipDto.setSalesRepresentativeId(transactionPartnerDto.getPartner());

                    }

                }

                if (transactionPartnerDto.getFunction().equals(PartnerFunction.CUSTOMER)) {
                    transactionDto.setCustomerId(transactionPartnerDto.getPartner());
                }
                if (transactionPartnerDto.getFunction().equals(PartnerFunction.SUPPLIER)) {
                    transactionDto.setSupplierId(transactionPartnerDto.getPartner());
                }
            }

            if (transactionEntity.getType().equals(TransactionType.MEMBERSHIP)) {


                TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
                transactionAmountPKEntity.setTransaction(transactionId);
                transactionAmountPKEntity.setType(PriceType.TOTAL_INC_VAT);
                TransactionAmountDto transactionAmountDto = getAmount(transactionAmountPKEntity);
                membershipDto.setPremium(transactionAmountDto.getAmount());
            }

            for (TransactionDateDto transactionDateDto : getDates(transactionId)) {

                if (transactionEntity.getType().equals(TransactionType.MEMBERSHIP)) {
                    if (transactionDateDto.getType().equals(DateType.JOINED)) {
                        membershipDto.setDateJoined(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equals(DateType.EFFECTIVE)) {
                        membershipDto.setDateEffective(transactionDateDto.getValue());
                    }
                }


                if (transactionDateDto.getType().equals(DateType.ORDER_DATE)) {
                    transactionDto.setOrderDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.INVOICE_DATE)) {
                    transactionDto.setInvoiceDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.DELIVERY_DATE)) {
                    transactionDto.setDeliveryDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.EXPIRY_DATE)) {
                    transactionDto.setExpiryDate(transactionDateDto.getValue());
                }
                if (transactionDateDto.getType().equals(DateType.DUE_DATE)) {
                    transactionDto.setDueDate(transactionDateDto.getValue());
                }
            }
            transactionDto.setMembershipHolder(membershipDto);
            return transactionDto;
        } else {
            throw new TransactionNotFound("Transaction not found");
        }

    }

    @Override
    public void addItem(TransactionItemDto transactionItemDto) throws Exception {
        try {
            TransactionItemEntity transactionItemEntity = new TransactionItemEntity(transactionItemDto);
            String itemUUID = UUID.randomUUID().toString().replace("-", "");
            transactionItemEntity.getTransactionItemPKEntity().setItem(itemUUID);
            transactionItemEntity.setUnitPrice(transactionItemDto.getUnitPrice());
            transactionItemEntity.setQuantity(transactionItemDto.getQuantity());
            transactionItemEntity.setUnitOfMeasure(transactionItemDto.getBaseUnitOfMeasure());
            transactionItemEntity.setValidFrom(new Date());
            transactionItemEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionItemRepository.save(transactionItemEntity);
            calculatePricing(transactionItemDto.getTransaction());
        } catch (Exception exception) {
            throw new Exception("Error adding item to transaction");
        }
    }

    @Override
    public void removeItem(TransactionItemDto transactionItemDto) throws Exception {
        TransactionItemPKEntity transactionItemPKEntity = new TransactionItemPKEntity();
        transactionItemPKEntity.setTransaction(transactionItemDto.getTransaction());
        transactionItemPKEntity.setItem(transactionItemDto.getItem());
        try {
            transactionItemRepository.deleteById(transactionItemPKEntity);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }

    @Override
    public List<TransactionItemDto> getItems(String id) {
        List<TransactionItemDto> transactionItemDtos = new ArrayList<>();
        List<TransactionItemEntity> transactionItemEntities = transactionItemRepository.getTransactionItems(id);
        for (TransactionItemEntity transactionItemEntity : transactionItemEntities) {
            transactionItemDtos.add(new TransactionItemDto(transactionItemEntity));
        }
        return transactionItemDtos;
    }

    @Override
    public void addAmount(TransactionAmountDto transactionAmountDto) {
        try {
            TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
            transactionAmountPKEntity.setTransaction(transactionAmountDto.getTransaction());
            transactionAmountPKEntity.setType(transactionAmountDto.getType());
            TransactionAmountEntity transactionAmountEntity = new TransactionAmountEntity();
            transactionAmountEntity.setTransactionAmountPKEntity(transactionAmountPKEntity);
            transactionAmountEntity.setAmount(transactionAmountDto.getAmount());
            transactionAmountRepository.save(transactionAmountEntity);
        } catch (Exception exception) {

        }
    }

    @Override
    public void removeAmount(TransactionAmountDto transactionAmountDto) throws Exception {
        TransactionAmountPKEntity transactionAmountPKEntity = new TransactionAmountPKEntity();
        transactionAmountPKEntity.setTransaction(transactionAmountDto.getTransaction());
        transactionAmountPKEntity.setType(transactionAmountDto.getType());
        try {
            transactionAmountRepository.deleteById(transactionAmountPKEntity);
        } catch (Exception ex) {
            throw new Exception("");
        }
    }

    @Override
    public List<TransactionAmountDto> getAmounts(String id) {
        List<TransactionAmountDto> transactionAmountDtos = new ArrayList<>();
        List<TransactionAmountEntity> transactionAmountEntities = transactionAmountRepository.getTransactionAmounts(id);
        for (TransactionAmountEntity transactionAmountEntity : transactionAmountEntities) {
            transactionAmountDtos.add(new TransactionAmountDto(transactionAmountEntity));
        }
        return transactionAmountDtos;
    }

    @Override
    public void addPartner(TransactionPartnerDto transactionPartnerDto) throws Exception {
        try {
            TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
            transactionPartnerPKEntity.setTransaction(transactionPartnerDto.getTransaction());
            transactionPartnerPKEntity.setFunction(transactionPartnerDto.getFunction());
            transactionPartnerPKEntity.setPartner(transactionPartnerDto.getPartner());

            TransactionPartnerEntity transactionPartnerEntity = new TransactionPartnerEntity();
            transactionPartnerEntity.setTransactionPartnerPKEntity(transactionPartnerPKEntity);
            transactionPartnerEntity.setValidFrom(new Date());
            transactionPartnerEntity.setCreatedBy(userService.getCurrentUser());
            transactionPartnerEntity.setStatus(transactionPartnerDto.getStatus());

            transactionPartnerRepository.save(transactionPartnerEntity);
        } catch (Exception exception) {
            throw new Exception("Could not add partner to transaction");
        }
    }

    @Override
    public void removePartner(TransactionPartnerDto transactionPartnerDto) throws Exception {
        TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
        transactionPartnerPKEntity.setTransaction(transactionPartnerDto.getTransaction());
        transactionPartnerPKEntity.setFunction(transactionPartnerDto.getFunction());
        transactionPartnerPKEntity.setPartner(transactionPartnerDto.getPartner());
        try {
            transactionPartnerRepository.deleteById(transactionPartnerPKEntity);
        } catch (Exception ex) {
            throw new Exception("Could not remove partner to transaction");
        }
    }

    private void calculatePricing(String id) throws Exception {
        try {
            List<LineItemDto> lineItemDtoList = new ArrayList<>();
            for (TransactionItemDto transactionItemDto : getItems(id)) {
                LineItemDto lineItemDto = new LineItemDto();
                lineItemDto.setQuantity(transactionItemDto.getQuantity());
                lineItemDto.setUnitPrice(transactionItemDto.getUnitPrice());
                lineItemDtoList.add(lineItemDto);
            }

            PricingDto pricingDto = pricingService.calculate(lineItemDtoList);

            List<TransactionAmountDto> transactionAmountDtoList = getAmounts(id);

            TransactionAmountDto totalIncVat = new TransactionAmountDto(id, PriceType.TOTAL_INC_VAT, pricingDto.getTotalIncVat());
            removeAmount(totalIncVat);
            addAmount(totalIncVat);

            TransactionAmountDto totalExcVat = new TransactionAmountDto(id, PriceType.TOTAL_EXC_VAT, pricingDto.getTotalExcVat());
            removeAmount(totalExcVat);
            addAmount(totalExcVat);

            TransactionAmountDto discountAmount = new TransactionAmountDto(id, PriceType.DISCOUNT_AMOUNT, pricingDto.getDiscountAmount());
            removeAmount(discountAmount);
            addAmount(discountAmount);

            TransactionAmountDto discountPercentage = new TransactionAmountDto(id, PriceType.DISCOUNT_PERCENT, pricingDto.getDiscountPercentage());
            removeAmount(discountPercentage);
            addAmount(discountPercentage);

            TransactionAmountDto VATAmount = new TransactionAmountDto(id, PriceType.VAT_AMOUNT, pricingDto.getVATAmount());
            removeAmount(VATAmount);
            addAmount(VATAmount);

            TransactionAmountDto VATPercentage = new TransactionAmountDto(id, PriceType.VAT_PERCENT, pricingDto.getVATPercentage());
            removeAmount(VATPercentage);
            addAmount(VATPercentage);

        } catch (Exception exception) {
            throw new Exception("Pricing Engine Failure");
        }
    }

}
