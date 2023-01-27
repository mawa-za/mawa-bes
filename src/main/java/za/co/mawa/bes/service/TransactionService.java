package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.TransactionDateRepository;
import za.co.mawa.bes.repository.TransactionPartnerRepository;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.dao.TransactionDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService implements TransactionDao {
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    TransactionPartnerRepository transactionPartnerRepository;
    @Autowired
    UserService userService;
    @Override
    public String create(TransactionDto transactionDto) {
        TransactionEntity transactionEntity = new TransactionEntity();
        String id = null;
        try {
            id = numberRangeService.generateNumber(transactionEntity.getType());
            transactionEntity.setId(id);
            if (transactionDto.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setType(transactionDto.getType());
            transactionEntity.setSubType(transactionDto.getSubType());
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionEntity.setCreatedBy(userService.getCurrentUser());
            transactionRepository.save(transactionEntity);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(id);
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);

        } catch (NumberRangeObjectNotFound ex) {
            throw new RuntimeException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void addDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
        transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        transactionDatePKEntity.setType(transactionDateDto.getType());
        TransactionDateEntity transactionDateEntity = new TransactionDateEntity();
        transactionDateEntity.setTransactionDatePK(transactionDatePKEntity);
        if (transactionDateDto.getValue() != null) {
            transactionDateEntity.setValue(Conversion.stringToDate(transactionDateDto.getValue()));
        } else {
            transactionDateEntity.setValue(new Date());
        }
        try {
            transactionDateRepository.save(transactionDateEntity);
        } catch (Exception ex) {
            throw new Exception("Date not added");
        }

    }

    @Override
    public void removeDate(TransactionDateDto transactionDateDto) throws Exception {
        TransactionDatePKEntity transactionDatePKEntity = new TransactionDatePKEntity();
        transactionDatePKEntity.setTransaction(transactionDateDto.getTransaction());
        transactionDatePKEntity.setType(transactionDateDto.getType());
        try{
        transactionDateRepository.deleteById(transactionDatePKEntity);
        } catch (Exception ex) {
            throw new Exception("Date not added");
        }
    }

    @Override
    public ArrayList<TransactionDateDto> getDates(String id) {
        return null;
    }

    @Override
    public void addAttachment(TransactionAttachmentDto transactionAttachmentDto) {

    }

    @Override
    public void removeDate(TransactionAttachmentDto transactionAttachmentDto) {

    }


    @Override
    public ArrayList<TransactionAttachmentDto> getAttachments(String id) {
        return null;
    }

    @Override
    public void addLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public void removeLink(TransactionLinkDto transactionLinkDto) {

    }

    @Override
    public ArrayList<TransactionLinkDto> getLinks(String id) {
        return null;
    }

    @Override
    public ArrayList<TransactionPartnerDto> getPartners(String transactionId) {
        ArrayList<TransactionPartnerDto> transactionPartnerDtos = new ArrayList<>();
        List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findPartnerByTransaction(transactionId);
        for (TransactionPartnerEntity transactionPartner : transactionPartnerEntities) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setFunction(transactionPartner.getTransactionPartnerPK().getPartnerFunction());
            transactionPartnerDto.setPartner(transactionPartner.getTransactionPartnerPK().getPartnerNo());
            transactionPartnerDto.setStatus(transactionPartner.getStatus());
            transactionPartnerDto.setStatusReason(transactionPartner.getStatusReason());
            transactionPartnerDto.setDateAdded(Conversion.dateToString(transactionPartner.getDateAdded()));
            transactionPartnerDto.setDateEffective(Conversion.dateToString(transactionPartner.getDateEffective()));
            if (transactionPartner.getCreatedBy() != null) {
                transactionPartnerDto.setCreatedBy(transactionPartner.getCreatedBy());
            }
            if (transactionPartner.getChangedBy() != null) {
                transactionPartnerDto.setChangedBy(transactionPartner.getChangedBy());
            }
            transactionPartnerDtos.add(transactionPartnerDto);
        }
        return transactionPartnerDtos;
    }

    @Override
    public ArrayList<TransactionDto> search(TransactionQueryDto query) {
        ArrayList<TransactionDto> transactionDtos = new ArrayList<>();
        if (query.getPartnerFunction() != null && query.getPartnerNo() != null) {
            List<TransactionPartnerEntity> transactionPartnerEntities = transactionPartnerRepository.findTransactionByPartner(query.getPartnerNo());
            for (TransactionPartnerEntity transactionPartnerEntity : transactionPartnerEntities) {
                if (transactionPartnerEntity.getTransactionPartnerPK().getPartnerFunction().equals(query.getPartnerFunction())) {
                    TransactionDto object = getTransaction(transactionPartnerEntity.getTransactionPartnerPK().getTransactionId());
                    transactionDtos.add(object);
                }
            }
        }

        if (query.getType() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByType(query.getType());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = getTransaction(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }

        if (query.getStatus() != null) {
            List<TransactionEntity> transactions = transactionRepository.findTransactionByStatus(query.getStatus());
            for (TransactionEntity transactionEntity : transactions) {
                TransactionDto transactionDto = getTransaction(transactionEntity.getId());
                transactionDtos.add(transactionDto);
            }
        }

        return transactionDtos;
    }

    @Override
    public void edit(TransactionDto transactionDto) {

    }

    @Override
    public TransactionDto getTransaction(String orderId) {
        TransactionDto transactionDto = null;
        TransactionEntity transactionEntity = transactionRepository.getById(orderId);
        if (transactionEntity != null) {
            transactionDto = new TransactionDto();
            transactionDto.setId(transactionEntity.getId());
            if (transactionEntity.getSubType() != null) {
                transactionDto.setSubType(transactionEntity.getSubType());
            }
            if (transactionEntity.getLocation() != null) {
                transactionDto.setLocation(transactionEntity.getLocation());
            }
            if (transactionEntity.getType() != null) {
                transactionDto.setType(transactionEntity.getType());
            }
            transactionDto.setStatus(StringConversion.capitalizeFully(transactionEntity.getStatus().replaceAll("_", " ")));
            if (transactionEntity.getStatusReason() != null) {
                transactionDto.setStatusReason(StringConversion.capitalizeFully(transactionEntity.getStatusReason().replaceAll("_", " ")));
            }
            if (transactionEntity.getSubStatus() != null) {
                transactionDto.setSubStatus(StringConversion.capitalizeFully(transactionEntity.getSubStatus()));
            }
            if (transactionEntity.getDescription() != null) {
                transactionDto.setDescription(transactionEntity.getDescription());
            }
            if (transactionEntity.getSubDescription() != null) {
                transactionDto.setSubDescription(transactionEntity.getSubDescription());
            }
            if (transactionEntity.getValidTo() != null) {
                transactionDto.setValidTo(Conversion.dateToString(transactionEntity.getValidTo()));
            }
            if (transactionEntity.getValidFrom() != null) {
                transactionDto.setValidFrom(Conversion.dateToString(transactionEntity.getValidFrom()));
            }
            if (transactionEntity.getCreatedBy() != null) {
                transactionDto.setCreatedBy(transactionEntity.getCreatedBy());
            }
            if (transactionEntity.getChangedBy() != null) {
                transactionDto.setChangedBy(transactionEntity.getChangedBy());
            }
        }
        return transactionDto;
    }

    @Override
    public void addItem(TransactionItemDto transactionItemDto) {

    }

    @Override
    public void removeItem(TransactionItemDto transactionItemDto) {

    }

    @Override
    public ArrayList<TransactionItemDto> getItems(String id) {
        return null;
    }

    @Override
    public void addAmount(TransactionAmountDto transactionAmountDto) {

    }

    @Override
    public void removeAmount(TransactionAmountDto transactionAmountDto) {

    }

    @Override
    public ArrayList<TransactionAmountDto> getAmounts(String id) {
        return null;
    }

    @Override
    public void addPartner(TransactionPartnerDto transactionPartnerDto) {
        try {
            TransactionPartnerPKEntity transactionPartnerPKEntity = new TransactionPartnerPKEntity();
            transactionPartnerPKEntity.setTransactionId(transactionPartnerDto.getTransaction());
            transactionPartnerPKEntity.setPartnerFunction(transactionPartnerDto.getFunction());
            transactionPartnerPKEntity.setPartnerNo(transactionPartnerDto.getPartner());

            TransactionPartnerEntity transactionPartnerEntity = new TransactionPartnerEntity();
            transactionPartnerEntity.setTransactionPartnerPK(transactionPartnerPKEntity);
            transactionPartnerEntity.setValidFrom(new Date());
            transactionPartnerEntity.setCreatedBy(userService.getCurrentUser());
            transactionPartnerEntity.setStatus(transactionPartnerDto.getStatus());

            transactionPartnerRepository.save(transactionPartnerEntity);
        }catch (Exception exception){

        }
    }

    @Override
    public void removePartner(TransactionPartnerDto transactionPartnerDto) {

    }


}
