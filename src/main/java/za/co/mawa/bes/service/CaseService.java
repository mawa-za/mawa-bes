package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DateDto;
import za.co.mawa.bes.dto.cas.CaseCreateDto;
import za.co.mawa.bes.dto.cas.CaseDto;
import za.co.mawa.bes.dto.cas.CaseQueryDto;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.participant.ParticipantDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.task.TaskDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class CaseService {
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
    CommentService commentService;
    @Autowired
    TaskService taskService;

    public CaseDto create(CaseCreateDto caseCreateDto) throws PartnerNotFoundException, ProductNotFoundException,
            TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.CASE);
//        transactionCreateDto.setLocation(caseCreateDto.getCourt());
        //this is the case type
//        transactionCreateDto.setSubType(caseCreateDto.getType());
        transactionCreateDto.setDescription(caseCreateDto.getDescription());
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);

       //CLASSIFICATION AS PRODUCT
//        if(caseCreateDto.getProduct() != null && !caseCreateDto.getProduct().isEmpty()) {
//            ProductDto productDto = productService.get(caseCreateDto.getProduct());
//            TransactionItemDto transactionItemDto = new TransactionItemDto();
//            transactionItemDto.setTransaction(transactionDto.getId());
//            transactionItemDto.setProduct(caseCreateDto.getProduct());
//            transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure().getCode());
//            transactionItemDto.setQuantity(new BigDecimal("1"));
//            transactionService.addItem(transactionItemDto);
//            List<ProductPricingDto> productPricingDtoList = productDto.getPricings().stream().toList();
//                    //.filter(a -> Objects.equals(a.getPricing().getCode(), ProductPricing.SELLING_PRICE))
//                    //.toList();
//            if (productPricingDtoList.iterator().hasNext()) {
//                TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
//                transactionAmountInboundDto.setAmount(productPricingDtoList.iterator().next().getValue());
//                transactionAmountInboundDto.setTransaction(transactionDto.getId());
//                transactionAmountInboundDto.setType(AmountType.SERVICE_AMOUNT);
//                transactionAmountService.save(transactionAmountInboundDto);
//            }
//        }
        if (caseCreateDto.getClient() != null && !caseCreateDto.getClient().isEmpty()) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.CLIENT);
            transactionPartnerDto.setPartner(caseCreateDto.getClient());
            transactionService.addPartner(transactionPartnerDto);
        }
//        if(!caseCreateDto.getApplicants().isEmpty()){
//            for (String applicant : caseCreateDto.getApplicants()) {
//                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
//                transactionPartnerDto.setTransaction(transactionDto.getId());
//                transactionPartnerDto.setFunction(PartnerFunction.APPLICANT);
//                transactionPartnerDto.setPartner(applicant);
//                transactionService.addPartner(transactionPartnerDto);
//            }
//        }
//        if(!caseCreateDto.getDefendants().isEmpty()){
//            for (String defendant : caseCreateDto.getDefendants()) {
//                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
//                transactionPartnerDto.setTransaction(transactionDto.getId());
//                transactionPartnerDto.setFunction(PartnerFunction.DEFENDANT);
//                transactionPartnerDto.setPartner(defendant);
//                transactionService.addPartner(transactionPartnerDto);
//            }
//        }
        CaseDto caseDto = new CaseDto();
        caseDto.setId(transactionDto.getId());
        return caseDto;
    }

    public List<CaseDto> search(CaseQueryDto caseQueryDto) {
        List<CaseDto> caseDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.CASE);
        for (String id : transactionService.search(transactionQueryDto)) {

            try {
                caseDtoList.add(get(id));
            }catch (Exception e){

            }
        }
        return caseDtoList;
    }

    public CaseDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            CaseDto caseDto = new CaseDto();
            caseDto.setId(transactionDto.getId());
            caseDto.setNumber(transactionDto.getNumber());
            caseDto.setDescription(transactionDto.getDescription());

            caseDto.setType(fieldOptionService.getFieldOption(TransactionType.CASE, transactionDto.getType()));
            caseDto.setCaseType(fieldOptionService.getFieldOption(Field.CASE_TYPE,transactionDto.getSubType()));
            caseDto.setCourt(fieldOptionService.getFieldOption(Field.COURT, transactionDto.getLocation()));

            List<ParticipantDto> participantDtoList = new ArrayList<>();
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.CLIENT)) {
                        caseDto.setClient(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.APPLICANT)) {
                        caseDto.getApplicants().add(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.DEFENDANT)) {
                        caseDto.getDefendants().add(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.SERVICE_PROVIDER)) {
                        caseDto.getServiceProviders().add(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    ParticipantDto participantDto = new ParticipantDto();
                    participantDto.setPartner(partnerService.get(transactionPartnerDto.getPartner()));
                    participantDto.setFunction(fieldOptionService.getFieldOption(Field.PARTNER_FUNCTION, transactionPartnerDto.getFunction()));
                    participantDtoList.add(participantDto);
                } catch (PartnerNotFoundException e) {

                }
            }
            //
            if (transactionService.getItems(transactionDto.getId()).iterator().hasNext()) {
                String productId = transactionService.getItems(transactionDto.getId()).iterator().next().getProduct();
                try {
                    caseDto.setProduct(productService.getBasic(productId));
                } catch (ProductNotFoundException e) {
                }
            }
            caseDto.setParticipants(participantDtoList);
            List<DateDto> dateDtoList = new ArrayList<>();
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                DateDto dateDto = new DateDto();
                dateDto.setValue(transactionDateDto.getValue());
                dateDto.setType(fieldOptionService.getFieldOption(Field.DATE_TYPE, transactionDateDto.getType()));
                dateDtoList.add(dateDto);
            }

            List<TransactionLinkDto> links = transactionService.getLinks(id);
            List<CommentDto> comments = new ArrayList<>();
            List<TaskDto> tasks = new ArrayList<>();

            for (TransactionLinkDto link : links) {
               try {
                   if (link.getType().equalsIgnoreCase(TransactionType.COMMENT)) {
//                       CommentDto comment = new CommentDto();
//                       comment = commentService.get(link.getTransaction2());
                       comments.add(commentService.get(link.getTransaction2()));

                   } else if (link.getType().equalsIgnoreCase(TransactionType.TASK)) {

                       tasks.add(taskService.get(link.getTransaction2()));
                   }
               } catch (Exception e) {
//                   throw new RuntimeException(e);
               }
            }
            caseDto.setComments(comments);
            caseDto.setTasks(tasks);

            caseDto.setDates(dateDtoList);
            caseDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            caseDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            return caseDto;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
