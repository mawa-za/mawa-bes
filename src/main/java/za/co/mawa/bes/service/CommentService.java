package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.cashup.CashupEditDto;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.comment.CommentInboundDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.utils.*;


import java.math.BigDecimal;
import java.util.*;

@Service
public class CommentService {

    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PartnerService partnerService;


    public String create(CommentInboundDto commentInboundDto) throws Exception {

        String id = null;
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.COMMENT);
        transactionCreateDto.setSubType("GENERAL");
        //set as the parent transaction for now
        transactionCreateDto.setLocation(commentInboundDto.getParentTransactionId());
        transactionCreateDto.setDescription(commentInboundDto.getDescription());
        TransactionDto transaction = transactionService.create(transactionCreateDto);

        if(transaction.getId() !=null){

            id=transaction.getId();
            TransactionPartnerDto employee = new TransactionPartnerDto();
            employee.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
            employee.setTransaction(transaction.getId());
            employee.setPartner(getUser());
            //employee.setPartner("ff8080818b18a25a018b18a88f5a0000");
            employee.setStatus(Status.ACTIVE);
            transactionService.addPartner(employee);

            TransactionDateDto date = new TransactionDateDto();
            date.setType(DateType.CREATED);
            date.setTransaction(id);
            date.setValue(new Date());
            transactionService.addDate(date);

            date = new TransactionDateDto();
            date.setType(DateType.LAST_UPDATED);
            date.setTransaction(id);
            date.setValue(new Date());
            transactionService.addDate(date);
        }

        return id ;
    }



    public CommentDto get(String id) throws Exception, DoesNotExist {
        TransactionDto transactionDto = transactionService.get(id);
        if (transactionDto.getType().equalsIgnoreCase(TransactionType.COMMENT)) {
            try {
                CommentDto commentDto = new CommentDto();
                commentDto.setType(transactionDto.getType());
                //can be returned as a field option in future
                commentDto.setSubType(transactionDto.getSubType());
                commentDto.setId(transactionDto.getId());
                //the id of the parent transaction
                commentDto.setParentTransactionId(transactionDto.getLocation());
                commentDto.setDescription(transactionDto.getDescription());

                for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
                    if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                        commentDto.setCreatedBy(partnerService.get(transactionPartner.getPartner()));
                    }
                }

                for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                    if (transactionDateDto.getType().equalsIgnoreCase(DateType.CREATED)) {
                        commentDto.setCreatedDate(transactionDateDto.getValue());
                    }
                    if (transactionDateDto.getType().equalsIgnoreCase(DateType.LAST_UPDATED)) {
                        commentDto.setLastUpdated(transactionDateDto.getValue());
                    }
                }
                return commentDto;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        } else {
            throw new DoesNotExist();
        }

    }

    public List<CommentDto> getAll(String id) throws Exception {
        List<TransactionEntity> transactionEntities = transactionRepository.findTransactionByLocation(id, "COMMENT");

        List<CommentDto> commentDtos = new ArrayList<>();
        for (TransactionEntity transactionEntity : transactionEntities) {

            CommentDto commentDto = get(transactionEntity.getId());
            commentDtos.add(commentDto);
        }

        return commentDtos;
    }


    public boolean edit(CommentInboundDto commentInboundDto, String id) throws Exception {
        TransactionEntity entity = transactionRepository.getById(id);
        boolean edited = false;
        String partnerId = null;
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                partnerId = partnerService.get(transactionPartner.getPartner()).getId();
            }
        }
        //if(Objects.equals(getUser(), partnerId)) {
            try {
                if (entity.getDescription() != null) {
                    entity.setDescription(commentInboundDto.getDescription());
                }
                TransactionDateEdit editDate = new TransactionDateEdit();
                editDate.setTransaction(id);
                editDate.setType(DateType.LAST_UPDATED);
                editDate.setValue(new Date());
                transactionService.dateEdit(editDate);
                entity.setChangedBy(getUser());
                transactionRepository.save(entity);
                edited = true;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        //}
        return edited;
    }

    public boolean delete(String id) throws Exception {

        TransactionEntity entity = transactionRepository.getById(id);
        boolean deleted = false;
        String partnerId = null;
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                partnerId = partnerService.get(transactionPartner.getPartner()).getId();
            }
        }
        //if(Objects.equals(getUser(), partnerId)) {
            try {
                 transactionRepository.deleteById(id);
                 deleted = true;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        //}

        return deleted;
    }
    public boolean deleteAll(String id) throws Exception {
        List<TransactionEntity> transactionEntities = transactionRepository.findTransactionByLocation(id, "COMMENT");
        boolean deleted = false;
        for (TransactionEntity transactionEntity : transactionEntities) {

            delete(transactionEntity.getId());
            deleted = true;
        }

        return deleted;
    }
    public String getUser() {
        try {

            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserEntity user = userRepository.getByName(userDetails.getUsername());

            if (user != null) {

                return String.valueOf(user.getPartner());
            } else {
                return null;
            }
        } catch (Exception e) {

            return null;
        }
    }

}
